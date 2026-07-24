"""Unit tests for the SQLAlchemy Core repository base class.

Exercises ``repositories.sql.SqlAlchemyRepository`` — the abstract,
``Engine``-backed implementation of the four-method ``IRepository`` contract —
directly, using two throwaway concrete subclasses defined here: ``_WidgetRepository``
(a single-column primary key) and ``_PairRepository`` (a two-column composite
key). Between them they cover both shapes the base class must support: only
``Badge`` has a scalar key in production, while ``Friendship``, ``AwardedBadge``
and ``Attendance`` are composite, so the composite path matters most.

Everything runs against a shared in-memory SQLite database, the same dialect the
wider suite (and #99's integration tests) will use, so no external infrastructure
is needed.

Coverage, method by method:
- construction: the abstract base and any incomplete subclass cannot be instantiated.
- ``save``: inserts a new row, round-trips every field, behaves as an upsert
  (overwrite by id, never duplicate), leaves sibling rows untouched, and rolls
  back atomically when the insert half fails.
- ``delete``: removes the targeted row only, and raises ``KeyError`` (carrying the
  id) when nothing matched — for both key shapes.
- ``find_by_id``: returns the entity when present, ``None`` when absent, and picks
  the right row among many — for both key shapes.
- ``find_all``: empty list when empty, every row otherwise, reflecting deletes.
- persistence: a fresh repository instance over the same engine reads rows a
  previous instance wrote, proving state lives in the database, not the object.

Run from the repo root:  ``python -m pytest api-core/tests/``
"""

from __future__ import annotations

import dataclasses
import datetime
import uuid

import pytest
import sqlalchemy
import sqlalchemy.exc

import attendance
from repositories import IEntity, sql

# ---------------------------------------------------------------------------
# Throwaway schema + entities + concrete repositories used only by these tests.
# ---------------------------------------------------------------------------
_metadata = sqlalchemy.MetaData()

# A single-column primary key, standing in for the Badge-style scalar id.
_widgets = sqlalchemy.Table(
    "widgets",
    _metadata,
    sqlalchemy.Column("id", sqlalchemy.Uuid, primary_key=True),
    sqlalchemy.Column("name", sqlalchemy.String, nullable=False),
)

# A two-column composite primary key, standing in for the (id, id) tuple keys of
# Attendance / AwardedBadge / Friendship.
_pairs = sqlalchemy.Table(
    "pairs",
    _metadata,
    sqlalchemy.Column("left", sqlalchemy.Uuid, primary_key=True),
    sqlalchemy.Column("right", sqlalchemy.Uuid, primary_key=True),
    sqlalchemy.Column("label", sqlalchemy.String, nullable=False),
)


# Helper method constructs a new Attendance record
def _attendance(attendee_id: uuid.UUID | None = None,
                event_id: uuid.UUID | None = None) -> attendance.Attendance:
    
    attendee_id = attendee_id if attendee_id is not None else uuid.uuid4()
    event_id = event_id if event_id is not None else uuid.uuid4()

    return attendance.Attendance(
        attendee_id,
        event_id,

        # tz-aware UTC, like the canned repo
        datetime.datetime.now(datetime.timezone.utc),   
    )


@dataclasses.dataclass(frozen=True)
class _Widget(IEntity[uuid.UUID]):
    id: uuid.UUID
    name: str

    def get_id(self) -> uuid.UUID:
        return self.id


type _PairId = tuple[uuid.UUID, uuid.UUID]


@dataclasses.dataclass(frozen=True)
class _Pair(IEntity[_PairId]):
    left: uuid.UUID
    right: uuid.UUID
    label: str

    def get_id(self) -> _PairId:
        return (self.left, self.right)


class _WidgetRepository(sql.SqlAlchemyRepository[uuid.UUID, _Widget]):
    """Concrete single-key repository over the ``widgets`` table."""

    @property
    def _table(self) -> sqlalchemy.Table:
        return _widgets

    def _id_predicate(self, entity_id: uuid.UUID) -> sqlalchemy.ColumnElement[bool]:
        return _widgets.c.id == entity_id

    def _to_row(self, entity: _Widget) -> dict:
        return {"id": entity.id, "name": entity.name}

    def _from_row(self, row: sqlalchemy.Row) -> _Widget:
        return _Widget(id=row.id, name=row.name)


class _PairRepository(sql.SqlAlchemyRepository[_PairId, _Pair]):
    """Concrete composite-key repository over the ``pairs`` table."""

    @property
    def _table(self) -> sqlalchemy.Table:
        return _pairs

    def _id_predicate(self, entity_id: _PairId) -> sqlalchemy.ColumnElement[bool]:
        return sqlalchemy.and_(
            _pairs.c.left == entity_id[0], _pairs.c.right == entity_id[1]
        )

    def _to_row(self, entity: _Pair) -> dict:
        return {"left": entity.left, "right": entity.right, "label": entity.label}

    def _from_row(self, row: sqlalchemy.Row) -> _Pair:
        return _Pair(left=row.left, right=row.right, label=row.label)


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------
@pytest.fixture
def engine() -> sqlalchemy.Engine:
    """A fresh, shared in-memory SQLite engine with the test tables created.

    ``StaticPool`` keeps every connection pointed at the same in-memory database,
    so a value written through one ``engine.begin()`` block is visible to the
    next — and to a second repository instance built on the same engine.
    """

    engine = sqlalchemy.create_engine(
        "sqlite://",
        connect_args={"check_same_thread": False},
        poolclass=sqlalchemy.StaticPool,
    )
    _metadata.create_all(engine)
    sql.metadata.create_all(engine)
    yield engine
    engine.dispose()


@pytest.fixture
def widgets(engine: sqlalchemy.Engine) -> _WidgetRepository:
    return _WidgetRepository(engine)


@pytest.fixture
def pairs(engine: sqlalchemy.Engine) -> _PairRepository:
    return _PairRepository(engine)


@pytest.fixture
def attendance_repo(
    engine: sqlalchemy.Engine
) -> attendance.SQLAlchemyAttendanceRepository:
    return attendance.SQLAlchemyAttendanceRepository(engine)


def _widget(name: str = "widget") -> _Widget:
    """A widget with a fresh id and the given name."""

    return _Widget(id=uuid.uuid4(), name=name)


def _pair(label: str = "pair") -> _Pair:
    """A pair with fresh ids and the given label."""

    return _Pair(left=uuid.uuid4(), right=uuid.uuid4(), label=label)


# ---------------------------------------------------------------------------
# Construction / abstractness
# ---------------------------------------------------------------------------
class TestAbstractBase:

    def test_cannot_instantiate_abstract_base(self, engine):
        with pytest.raises(TypeError):
            sql.SqlAlchemyRepository(engine)

    def test_incomplete_subclass_cannot_be_instantiated(self, engine):
        # Implements every hook except _from_row, so it is still abstract.
        class _Partial(sql.SqlAlchemyRepository):
            @property
            def _table(self):
                return _widgets

            def _id_predicate(self, entity_id):
                return _widgets.c.id == entity_id

            def _to_row(self, entity):
                return {}

        with pytest.raises(TypeError):
            _Partial(engine)


# ---------------------------------------------------------------------------
# save  (single-column key)
# ---------------------------------------------------------------------------
class TestSave:

    def test_save_persists_a_new_entity(self, widgets):
        widget = _widget()

        widgets.save(widget)

        assert widgets.find_by_id(widget.id) == widget

    def test_save_round_trips_all_fields(self, widgets):
        widget = _widget(name="precisely this")

        widgets.save(widget)

        stored = widgets.find_by_id(widget.id)
        assert stored.id == widget.id
        assert stored.name == "precisely this"

    def test_save_is_an_upsert_overwriting_by_id(self, widgets):
        widget_id = uuid.uuid4()
        widgets.save(_Widget(id=widget_id, name="before"))

        widgets.save(_Widget(id=widget_id, name="after"))

        # Overwritten in place: the new value wins and no duplicate row appears.
        assert widgets.find_by_id(widget_id).name == "after"
        assert len(widgets.find_all()) == 1

    def test_save_leaves_other_rows_untouched(self, widgets):
        kept = _widget(name="kept")
        target = _widget(name="v1")
        widgets.save(kept)
        widgets.save(target)

        # Re-saving `target` must not disturb `kept` (save deletes only its own id).
        widgets.save(_Widget(id=target.id, name="v2"))

        assert widgets.find_by_id(kept.id) == kept
        assert widgets.find_by_id(target.id).name == "v2"

    def test_multiple_distinct_saves_all_persist(self, widgets):
        first, second, third = _widget(), _widget(), _widget()

        widgets.save(first)
        widgets.save(second)
        widgets.save(third)

        assert set(widgets.find_all()) == {first, second, third}

    def test_failed_insert_rolls_back_leaving_existing_row(self, widgets):
        original = _widget(name="original")
        widgets.save(original)

        # A NULL name violates the NOT NULL constraint, so the insert half of the
        # delete-then-insert fails; the whole transaction must roll back, undoing
        # the delete and leaving the original row intact.
        with pytest.raises(sqlalchemy.exc.IntegrityError):
            widgets.save(_Widget(id=original.id, name=None))

        assert widgets.find_by_id(original.id) == original


# ---------------------------------------------------------------------------
# save  (composite key)
# ---------------------------------------------------------------------------
class TestSaveCompositeKey:

    def test_save_persists_a_composite_key_entity(self, pairs):
        pair = _pair()

        pairs.save(pair)

        assert pairs.find_by_id(pair.get_id()) == pair

    def test_upsert_on_composite_key_overwrites_not_duplicates(self, pairs):
        left, right = uuid.uuid4(), uuid.uuid4()
        pairs.save(_Pair(left=left, right=right, label="before"))

        pairs.save(_Pair(left=left, right=right, label="after"))

        assert pairs.find_by_id((left, right)).label == "after"
        assert len(pairs.find_all()) == 1

    def test_entities_sharing_one_key_component_are_distinct(self, pairs):
        shared = uuid.uuid4()
        one = _Pair(left=shared, right=uuid.uuid4(), label="one")
        two = _Pair(left=shared, right=uuid.uuid4(), label="two")

        pairs.save(one)
        pairs.save(two)

        # Matching on only half the key would collapse these; both must survive.
        assert len(pairs.find_all()) == 2
        assert pairs.find_by_id(one.get_id()) == one
        assert pairs.find_by_id(two.get_id()) == two


# ---------------------------------------------------------------------------
# delete
# ---------------------------------------------------------------------------
class TestDelete:

    def test_delete_removes_an_existing_entity(self, widgets):
        widget = _widget()
        widgets.save(widget)

        widgets.delete(widget.id)

        assert widgets.find_by_id(widget.id) is None
        assert widgets.find_all() == []

    def test_delete_missing_id_raises_key_error(self, widgets):
        missing = uuid.uuid4()

        with pytest.raises(KeyError) as exc_info:
            widgets.delete(missing)

        # The raised KeyError names the id that was not found.
        assert exc_info.value.args[0] == missing

    def test_delete_only_removes_the_targeted_row(self, widgets):
        doomed = _widget()
        survivor = _widget()
        widgets.save(doomed)
        widgets.save(survivor)

        widgets.delete(doomed.id)

        assert widgets.find_by_id(doomed.id) is None
        assert widgets.find_by_id(survivor.id) == survivor

    def test_delete_missing_composite_key_raises_key_error(self, pairs):
        missing = (uuid.uuid4(), uuid.uuid4())

        with pytest.raises(KeyError) as exc_info:
            pairs.delete(missing)

        assert exc_info.value.args[0] == missing

    def test_delete_composite_removes_only_the_targeted_row(self, pairs):
        shared = uuid.uuid4()
        doomed = _Pair(left=shared, right=uuid.uuid4(), label="doomed")
        survivor = _Pair(left=shared, right=uuid.uuid4(), label="survivor")
        pairs.save(doomed)
        pairs.save(survivor)

        pairs.delete(doomed.get_id())

        assert pairs.find_by_id(doomed.get_id()) is None
        assert pairs.find_by_id(survivor.get_id()) == survivor


# ---------------------------------------------------------------------------
# find_by_id
# ---------------------------------------------------------------------------
class TestFindById:

    def test_returns_the_entity_when_present(self, widgets):
        widget = _widget()
        widgets.save(widget)

        assert widgets.find_by_id(widget.id) == widget

    def test_returns_none_when_absent(self, widgets):
        assert widgets.find_by_id(uuid.uuid4()) is None

    def test_returns_none_from_a_populated_repository(self, widgets):
        widgets.save(_widget())

        assert widgets.find_by_id(uuid.uuid4()) is None

    def test_selects_the_correct_entity_among_many(self, widgets):
        wanted = _widget(name="wanted")
        widgets.save(_widget(name="noise"))
        widgets.save(wanted)
        widgets.save(_widget(name="more noise"))

        assert widgets.find_by_id(wanted.id) == wanted

    def test_selects_the_correct_composite_entity_among_many(self, pairs):
        wanted = _pair(label="wanted")
        pairs.save(_pair())
        pairs.save(wanted)
        pairs.save(_pair())

        assert pairs.find_by_id(wanted.get_id()) == wanted


# ---------------------------------------------------------------------------
# find_all
# ---------------------------------------------------------------------------
class TestFindAll:

    def test_empty_repository_returns_empty_list(self, widgets):
        result = widgets.find_all()

        assert result == []
        assert isinstance(result, list)

    def test_returns_every_entity(self, widgets):
        saved = {_widget(), _widget(), _widget()}
        for widget in saved:
            widgets.save(widget)

        assert set(widgets.find_all()) == saved

    def test_reflects_deletes(self, widgets):
        kept = _widget()
        removed = _widget()
        widgets.save(kept)
        widgets.save(removed)

        widgets.delete(removed.id)

        assert widgets.find_all() == [kept]


# ---------------------------------------------------------------------------
# Persistence: state lives in the database, not the repository object
# ---------------------------------------------------------------------------
class TestPersistenceAcrossInstances:

    def test_a_fresh_instance_reads_a_row_written_by_another(self, engine):
        writer = _WidgetRepository(engine)
        widget = _widget()
        writer.save(widget)

        reader = _WidgetRepository(engine)

        assert reader.find_by_id(widget.id) == widget

    def test_a_fresh_instance_sees_all_rows(self, engine):
        writer = _WidgetRepository(engine)
        saved = {_widget(), _widget()}
        for widget in saved:
            writer.save(widget)

        reader = _WidgetRepository(engine)

        assert set(reader.find_all()) == saved
