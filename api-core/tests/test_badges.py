"""Unit tests for the badges slice.

Covers the ``predicates`` module on two fronts:

  * ``TestPredicateSerialisation``
        ``to_dict`` shape for a leaf and a nested composite, the non-trivial
        scalar conversions (UUID / datetime / timedelta / ``between``), the
        ``to_dict`` -> ``predicate_from_dict`` round-trip for every predicate
        type (including through a real JSON dump/load), and the ``ValueError``
        raised on an unknown or missing type tag.

  * Behavioural ``is_satisfied_by`` coverage
        ``TestMinFriends``, ``TestMinEventsAttended``,
        ``TestMinEventsInRollingWindow``, ``TestMinMessagesSent``,
        ``TestMinMessagesSentInRollingWindow`` exercise each leaf at, below and
        above its threshold plus its filter/window edge cases;
        ``TestBooleanCombinators`` covers the And/Or/Not truth tables and the
        ``&`` / ``|`` / ``~`` operator sugar; ``TestExampleBadges`` checks a few
        realistic composed award rules; ``TestAwardContextWindowing`` pins the
        ``since`` / ``until`` filtering (including the ``friend_count``
        state-fact pass-through).

It also covers the ``badge_service`` service layer:

  * ``TestBadgeService``
        the manual operations (``create_badge`` / ``award_badge`` /
        ``revoke_badge`` / ``has_badge`` / ``get_user_badges``) and the
        automatic, condition-driven ``evaluate_badges``, driven through the live
        module-level repository singletons (reset per test by the
        ``reset_badge_repositories`` autouse fixture in ``conftest.py``). The
        repeatable-badge evaluation cases build activity timestamped relative to
        the stored ``awarded_at`` so the ``context.since`` re-award window is
        exercised on both sides.

Run from the repo root:  ``python -m pytest api-core/tests/``
"""

import datetime
import json
import uuid

import pytest

import badges.awarded_badge_repository as awarded_badge_repository
import badges.base as base
from badges import badge_service
from badges.predicates import (
    AndPredicate,
    AttendedEvent,
    AwardContext,
    MinEventsAttended,
    MinEventsInRollingWindow,
    MinFriends,
    MinMessagesSent,
    MinMessagesSentInRollingWindow,
    NotPredicate,
    OrPredicate,
    predicate_from_dict,
)

_HOST = uuid.UUID("11111111-1111-1111-1111-111111111111")
_HOST_B = uuid.UUID("22222222-2222-2222-2222-222222222222")
_START = datetime.datetime(2026, 10, 31, 20, 0)
_END = datetime.datetime(2026, 11, 1, 6, 0)

# A fixed origin the behavioural tests place facts relative to, in minutes, so
# each test reads as an offset timeline rather than a wall of datetimes.
_T0 = datetime.datetime(2026, 1, 1, 12, 0)


def _event(offset_minutes=0, host_id=_HOST, categories=("music",)):
    """Builds an ``AttendedEvent`` ``offset_minutes`` after ``_T0``."""

    return AttendedEvent(
        event_id=uuid.uuid4(),
        host_id=host_id,
        categories=frozenset(categories),
        start_time=_T0 + datetime.timedelta(minutes=offset_minutes),
    )


def _context(events=(), friend_count=0, message_offsets=()):
    """Builds an ``AwardContext`` from event objects and message offsets.

    ``message_offsets`` are minutes-after-``_T0`` for each sent message, mirroring
    the ``offset_minutes`` convention used by ``_event``.
    """

    return AwardContext(
        user_id=uuid.uuid4(),
        attended_events=tuple(events),
        friend_count=friend_count,
        messages_sent=tuple(
            _T0 + datetime.timedelta(minutes=m) for m in message_offsets
        ),
    )

def _event_at(when, host_id=_HOST, categories=("music",)):
    """Build an ``AttendedEvent`` at an absolute ``when`` datetime.

    The service-layer re-award tests need attendances placed relative to a
    stored ``awarded_at`` (wall-clock ``now()``), which ``_event``'s
    ``_T0``-relative offsets cannot express.
    """

    return AttendedEvent(
        event_id=uuid.uuid4(),
        host_id=host_id,
        categories=frozenset(categories),
        start_time=when,
    )


def _service_context(user_id, events=(), friend_count=0):
    """Build an ``AwardContext`` for a specific ``user_id``.

    ``_context`` invents a fresh user each call, but the service tests need a
    context tied to the same user they award/query against.
    """

    return AwardContext(
        user_id=user_id,
        attended_events=tuple(events),
        friend_count=friend_count,
    )


def _award_record(user_id, badge_id):
    """Return the stored ``AwardedBadge`` for a pair, or ``None``."""

    return awarded_badge_repository._repository.find_by_id(
        base._generate_award_id(user_id, badge_id)
    )


# One instance of every predicate type, plus composites, exercising each
# non-trivial field conversion (UUID ``host_id``, ISO ``between`` bounds with
# every open/closed combination, and ``timedelta`` windows) and operator sugar.
_ALL_PREDICATES = [
    MinFriends(5),
    MinMessagesSent(50),
    MinMessagesSent(3, between=(_START, _END)),
    MinMessagesSentInRollingWindow(10, datetime.timedelta(hours=2)),
    MinEventsAttended(5, category="freshers"),
    MinEventsAttended(2, host_id=_HOST, category="club-night",
                      between=(_START, None)),
    MinEventsAttended(1, between=(None, _END)),
    MinEventsInRollingWindow(3, datetime.timedelta(days=7), host_id=_HOST,
                             category="club-night"),
    AndPredicate(MinFriends(5), MinMessagesSent(50)),
    OrPredicate(MinFriends(1), MinFriends(2)),
    NotPredicate(MinFriends(3)),
    (MinFriends(5) & MinMessagesSent(50)) | ~MinEventsAttended(1,
                                                               between=(None,
                                                                        _END)),
]


class TestPredicateSerialisation:
    """Serialisation contract for the predicate hierarchy."""

    def test_leaf_to_dict_shape(self):
        """A leaf emits its ``type`` tag alongside its parameters."""

        predicate = MinEventsAttended(5, category="freshers")

        assert predicate.to_dict() == {
            "type": "min_events_attended",
            "threshold": 5,
            "host_id": None,
            "category": "freshers",
            "between": None,
        }

    def test_composite_to_dict_shape(self):
        """A combinator nests its operands' dicts under its own tag."""

        predicate = AndPredicate(MinFriends(5), NotPredicate(MinFriends(1)))

        assert predicate.to_dict() == {
            "type": "and",
            "left": {"type": "min_friends", "threshold": 5},
            "right": {
                "type": "not",
                "operand": {"type": "min_friends", "threshold": 1},
            },
        }

    def test_scalar_conversions(self):
        """UUIDs, datetimes and timedeltas serialise to JSON-native forms."""

        events = MinEventsAttended(2, host_id=_HOST, between=(_START, _END))
        events_dict = events.to_dict()
        assert events_dict["host_id"] == str(_HOST)
        assert events_dict["between"] == [_START.isoformat(), _END.isoformat()]

        window = MinEventsInRollingWindow(3, datetime.timedelta(days=7))
        assert window.to_dict()["window"] == datetime.timedelta(
            days=7
        ).total_seconds()

    @pytest.mark.parametrize("predicate", _ALL_PREDICATES)
    def test_round_trip(self, predicate):
        """``predicate_from_dict(p.to_dict()) == p`` for every type."""

        assert predicate_from_dict(predicate.to_dict()) == predicate

    @pytest.mark.parametrize("predicate", _ALL_PREDICATES)
    def test_round_trip_through_json(self, predicate):
        """``to_dict`` is JSON-native, so a real dump/load round-trips too."""

        restored = predicate_from_dict(json.loads(json.dumps(predicate.to_dict())))
        assert restored == predicate

    def test_unknown_tag_raises(self):
        """An unregistered ``type`` tag is rejected."""

        with pytest.raises(ValueError):
            predicate_from_dict({"type": "definitely_not_a_predicate"})

    def test_missing_tag_raises(self):
        """A dict without a ``type`` tag is rejected."""

        with pytest.raises(ValueError):
            predicate_from_dict({"threshold": 5})


class TestMinFriends:
    """``MinFriends`` counts a state-style ``friend_count`` against a threshold."""

    def test_below_threshold_is_unsatisfied(self):
        assert not MinFriends(5).is_satisfied_by(_context(friend_count=4))

    def test_at_threshold_is_satisfied(self):
        assert MinFriends(5).is_satisfied_by(_context(friend_count=5))

    def test_above_threshold_is_satisfied(self):
        assert MinFriends(5).is_satisfied_by(_context(friend_count=9))

    def test_zero_friends_default_context(self):
        assert not MinFriends(1).is_satisfied_by(_context())


class TestMinEventsAttended:
    """``MinEventsAttended`` counts attendances under optional filters/window."""

    def test_below_threshold_is_unsatisfied(self):
        context = _context([_event(0), _event(10)])
        assert not MinEventsAttended(3).is_satisfied_by(context)

    def test_at_threshold_is_satisfied(self):
        context = _context([_event(0), _event(10), _event(20)])
        assert MinEventsAttended(3).is_satisfied_by(context)

    def test_above_threshold_is_satisfied(self):
        context = _context([_event(i) for i in range(5)])
        assert MinEventsAttended(3).is_satisfied_by(context)

    def test_empty_context_is_unsatisfied(self):
        assert not MinEventsAttended(1).is_satisfied_by(_context())

    def test_host_filter_counts_only_matching_host(self):
        context = _context(
            [_event(0, host_id=_HOST), _event(10, host_id=_HOST_B)]
        )
        # Only one of the two events is hosted by ``_HOST``.
        assert MinEventsAttended(1, host_id=_HOST).is_satisfied_by(context)
        assert not MinEventsAttended(2, host_id=_HOST).is_satisfied_by(context)

    def test_category_filter_counts_only_matching_category(self):
        context = _context(
            [
                _event(0, categories=("freshers",)),
                _event(10, categories=("sports",)),
                _event(20, categories=("freshers", "music")),
            ]
        )
        predicate = MinEventsAttended(2, category="freshers")
        assert predicate.is_satisfied_by(context)
        assert not MinEventsAttended(3, category="freshers").is_satisfied_by(
            context
        )

    def test_between_window_start_inclusive_end_exclusive(self):
        # ``start`` is applied via ``since`` (inclusive); ``end`` via ``until``
        # (exclusive). The window is [_T0, _T0 + 60min).
        window = (_T0, _T0 + datetime.timedelta(minutes=60))
        context = _context(
            [
                _event(-1),  # before the window
                _event(0),  # exactly the start -> included
                _event(30),  # inside
                _event(60),  # exactly the end -> excluded
            ]
        )
        # Two events fall in the window (offsets 0 and 30).
        assert MinEventsAttended(2, between=window).is_satisfied_by(context)
        assert not MinEventsAttended(3, between=window).is_satisfied_by(context)

    def test_open_ended_windows(self):
        context = _context([_event(-30), _event(0), _event(30)])
        # Open start: everything strictly before _T0 + 15min.
        open_start = (None, _T0 + datetime.timedelta(minutes=15))
        assert MinEventsAttended(2, between=open_start).is_satisfied_by(context)
        # Open end: everything at or after _T0.
        open_end = (_T0, None)
        assert MinEventsAttended(2, between=open_end).is_satisfied_by(context)
        assert not MinEventsAttended(3, between=open_end).is_satisfied_by(
            context
        )

    def test_combined_filters(self):
        context = _context(
            [
                # Matches host, category and window.
                _event(0, host_id=_HOST, categories=("club-night",)),
                # Right window/category, wrong host.
                _event(10, host_id=_HOST_B, categories=("club-night",)),
                # Right host/window, wrong category.
                _event(20, host_id=_HOST, categories=("quiz",)),
                # Right host/category, outside window.
                _event(500, host_id=_HOST, categories=("club-night",)),
            ]
        )
        predicate = MinEventsAttended(
            1,
            host_id=_HOST,
            category="club-night",
            between=(_T0, _T0 + datetime.timedelta(minutes=60)),
        )
        assert predicate.is_satisfied_by(context)
        # Only one event satisfies every filter, so a threshold of two fails.
        assert not MinEventsAttended(
            2,
            host_id=_HOST,
            category="club-night",
            between=(_T0, _T0 + datetime.timedelta(minutes=60)),
        ).is_satisfied_by(context)


class TestMinEventsInRollingWindow:
    """``MinEventsInRollingWindow`` slides a fixed-duration span for bursts."""

    _WINDOW = datetime.timedelta(minutes=90)

    def test_burst_within_window_is_satisfied(self):
        # Three events inside a 90-minute span, regardless of their absolute
        # position on a longer timeline.
        context = _context([_event(0), _event(200), _event(250), _event(280)])
        assert MinEventsInRollingWindow(3, self._WINDOW).is_satisfied_by(
            context
        )

    def test_events_spread_beyond_window_is_unsatisfied(self):
        # Two events 120 minutes apart never share a 90-minute window. This is
        # the case that previously ran off the end of the list (IndexError).
        context = _context([_event(0), _event(120)])
        assert not MinEventsInRollingWindow(2, self._WINDOW).is_satisfied_by(
            context
        )

    def test_boundary_span_equal_to_window_is_inclusive(self):
        # An event exactly ``window`` after the left edge is inside the window.
        inside = _context([_event(0), _event(90)])
        assert MinEventsInRollingWindow(2, self._WINDOW).is_satisfied_by(inside)
        # One minute more and it falls out.
        outside = _context([_event(0), _event(91)])
        assert not MinEventsInRollingWindow(2, self._WINDOW).is_satisfied_by(
            outside
        )

    def test_out_of_order_events_are_handled(self):
        # The predicate sorts chronologically, so input order must not matter.
        context = _context([_event(280), _event(0), _event(250), _event(200)])
        assert MinEventsInRollingWindow(3, self._WINDOW).is_satisfied_by(
            context
        )

    def test_host_and_category_filters_apply_within_window(self):
        context = _context(
            [
                _event(0, host_id=_HOST, categories=("club-night",)),
                _event(30, host_id=_HOST_B, categories=("club-night",)),
                _event(60, host_id=_HOST, categories=("club-night",)),
            ]
        )
        # Only two of the three in-window events match ``_HOST``.
        assert MinEventsInRollingWindow(
            2, self._WINDOW, host_id=_HOST
        ).is_satisfied_by(context)
        assert not MinEventsInRollingWindow(
            3, self._WINDOW, host_id=_HOST
        ).is_satisfied_by(context)
        assert not MinEventsInRollingWindow(
            2, self._WINDOW, category="quiz"
        ).is_satisfied_by(context)

    def test_empty_context_is_unsatisfied(self):
        assert not MinEventsInRollingWindow(1, self._WINDOW).is_satisfied_by(
            _context()
        )


class TestMinMessagesSent:
    """``MinMessagesSent`` counts message timestamps under an optional window."""

    def test_below_threshold_is_unsatisfied(self):
        assert not MinMessagesSent(3).is_satisfied_by(
            _context(message_offsets=(0, 10))
        )

    def test_at_threshold_is_satisfied(self):
        assert MinMessagesSent(3).is_satisfied_by(
            _context(message_offsets=(0, 10, 20))
        )

    def test_above_threshold_is_satisfied(self):
        assert MinMessagesSent(2).is_satisfied_by(
            _context(message_offsets=(0, 10, 20))
        )

    def test_between_window_start_inclusive_end_exclusive(self):
        window = (_T0, _T0 + datetime.timedelta(minutes=60))
        context = _context(message_offsets=(-1, 0, 30, 60))
        # Offsets 0 and 30 fall in [_T0, _T0 + 60min).
        assert MinMessagesSent(2, between=window).is_satisfied_by(context)
        assert not MinMessagesSent(3, between=window).is_satisfied_by(context)

    def test_empty_context_is_unsatisfied(self):
        assert not MinMessagesSent(1).is_satisfied_by(_context())


class TestMinMessagesSentInRollingWindow:
    """``MinMessagesSentInRollingWindow`` slides a span across message times."""

    _WINDOW = datetime.timedelta(minutes=90)

    def test_burst_within_window_is_satisfied(self):
        context = _context(message_offsets=(0, 200, 240, 260))
        assert MinMessagesSentInRollingWindow(3, self._WINDOW).is_satisfied_by(
            context
        )

    def test_messages_spread_beyond_window_is_unsatisfied(self):
        # 120 minutes apart cannot share a 90-minute window. This case
        # previously passed because the ``until`` filter dropped the ``since``.
        context = _context(message_offsets=(0, 120))
        assert not MinMessagesSentInRollingWindow(
            2, self._WINDOW
        ).is_satisfied_by(context)

    def test_boundary_span_equal_to_window_is_exclusive(self):
        # The window is [start, start + window): a message exactly ``window``
        # later is excluded.
        at_boundary = _context(message_offsets=(0, 90))
        assert not MinMessagesSentInRollingWindow(
            2, self._WINDOW
        ).is_satisfied_by(at_boundary)
        inside = _context(message_offsets=(0, 89))
        assert MinMessagesSentInRollingWindow(2, self._WINDOW).is_satisfied_by(
            inside
        )

    def test_below_threshold_is_unsatisfied(self):
        context = _context(message_offsets=(0, 10))
        assert not MinMessagesSentInRollingWindow(
            3, self._WINDOW
        ).is_satisfied_by(context)

    def test_empty_context_is_unsatisfied(self):
        assert not MinMessagesSentInRollingWindow(
            1, self._WINDOW
        ).is_satisfied_by(_context())


class TestBooleanCombinators:
    """And/Or/Not truth tables and the ``&`` / ``|`` / ``~`` operator sugar."""

    # ``MinFriends`` is a clean, deterministic switch: friend_count 1 satisfies
    # ``MinFriends(1)`` (true leaf) and fails ``MinFriends(9)`` (false leaf).
    _TRUE = MinFriends(1)
    _FALSE = MinFriends(9)
    _CONTEXT = _context(friend_count=1)

    @pytest.mark.parametrize(
        "left, right, expected",
        [
            (_TRUE, _TRUE, True),
            (_TRUE, _FALSE, False),
            (_FALSE, _TRUE, False),
            (_FALSE, _FALSE, False),
        ],
    )
    def test_and_truth_table(self, left, right, expected):
        assert AndPredicate(left, right).is_satisfied_by(self._CONTEXT) is (
            expected
        )

    @pytest.mark.parametrize(
        "left, right, expected",
        [
            (_TRUE, _TRUE, True),
            (_TRUE, _FALSE, True),
            (_FALSE, _TRUE, True),
            (_FALSE, _FALSE, False),
        ],
    )
    def test_or_truth_table(self, left, right, expected):
        assert OrPredicate(left, right).is_satisfied_by(self._CONTEXT) is (
            expected
        )

    def test_not_negates(self):
        assert NotPredicate(self._FALSE).is_satisfied_by(self._CONTEXT)
        assert not NotPredicate(self._TRUE).is_satisfied_by(self._CONTEXT)

    def test_operator_sugar_matches_explicit_construction(self):
        assert (self._TRUE & self._FALSE) == AndPredicate(
            self._TRUE, self._FALSE
        )
        assert (self._TRUE | self._FALSE) == OrPredicate(
            self._TRUE, self._FALSE
        )
        assert (~self._TRUE) == NotPredicate(self._TRUE)

    def test_operator_sugar_evaluates_correctly(self):
        assert not (self._TRUE & self._FALSE).is_satisfied_by(self._CONTEXT)
        assert (self._TRUE | self._FALSE).is_satisfied_by(self._CONTEXT)
        assert (~self._FALSE).is_satisfied_by(self._CONTEXT)

    def test_nested_composition(self):
        # (friends >= 1 AND friends >= 9) OR NOT(friends >= 9)
        rule = (self._TRUE & self._FALSE) | ~self._FALSE
        assert rule.is_satisfied_by(self._CONTEXT)

    def test_non_predicate_operand_raises(self):
        with pytest.raises(TypeError):
            self._TRUE & 5
        with pytest.raises(TypeError):
            self._TRUE | "not-a-predicate"


class TestExampleBadges:
    """A few realistic composed award rules, end to end."""

    def test_social_butterfly(self):
        # 10+ friends AND attended at least 3 events.
        rule = MinFriends(10) & MinEventsAttended(3)
        earns = _context([_event(i) for i in range(3)], friend_count=10)
        assert rule.is_satisfied_by(earns)
        # Enough events but too few friends.
        too_few_friends = _context([_event(i) for i in range(3)], friend_count=2)
        assert not rule.is_satisfied_by(too_few_friends)

    def test_freshers_regular(self):
        # 5 events tagged "freshers", regardless of host.
        rule = MinEventsAttended(5, category="freshers")
        earns = _context(
            [_event(i, categories=("freshers",)) for i in range(5)]
        )
        assert rule.is_satisfied_by(earns)
        mixed = _context(
            [_event(0, categories=("freshers",))]
            + [_event(i, categories=("sports",)) for i in range(1, 5)]
        )
        assert not rule.is_satisfied_by(mixed)

    def test_night_owl_burst(self):
        # 3 club-nights hosted by _HOST within any 7-day window.
        window = datetime.timedelta(days=7)
        rule = MinEventsInRollingWindow(
            3, window, host_id=_HOST, category="club-night"
        )
        day = 24 * 60
        earns = _context(
            [
                _event(0, host_id=_HOST, categories=("club-night",)),
                _event(2 * day, host_id=_HOST, categories=("club-night",)),
                _event(5 * day, host_id=_HOST, categories=("club-night",)),
            ]
        )
        assert rule.is_satisfied_by(earns)
        # Same three, but spread across more than a week.
        spread = _context(
            [
                _event(0, host_id=_HOST, categories=("club-night",)),
                _event(5 * day, host_id=_HOST, categories=("club-night",)),
                _event(10 * day, host_id=_HOST, categories=("club-night",)),
            ]
        )
        assert not rule.is_satisfied_by(spread)


class TestAwardContextWindowing:
    """``since`` / ``until`` filter time-stamped facts, pass state through."""

    def test_since_keeps_facts_at_or_after_cutoff(self):
        context = _context(
            [_event(-10), _event(0), _event(10)],
            message_offsets=(-10, 0, 10),
        )
        result = context.since(_T0)
        assert len(result.attended_events) == 2  # offsets 0 and 10
        assert len(result.messages_sent) == 2

    def test_until_keeps_facts_strictly_before_cutoff(self):
        context = _context(
            [_event(-10), _event(0), _event(10)],
            message_offsets=(-10, 0, 10),
        )
        result = context.until(_T0)
        assert len(result.attended_events) == 1  # only offset -10
        assert len(result.messages_sent) == 1

    def test_friend_count_passes_through_windowing(self):
        # A scalar state fact has no timestamp to filter on, so both windows
        # copy it through unchanged (the documented repeatable-badge caveat).
        context = _context(friend_count=7)
        assert context.since(_T0).friend_count == 7
        assert context.until(_T0).friend_count == 7

    def test_windowing_returns_a_new_context(self):
        context = _context([_event(0)])
        assert context.since(_T0) is not context
        assert context.until(_T0) is not context


class TestBadgeService:
    """Service-layer behaviour for ``badges.badge_service``.

    Runs against the live ``badge_repository`` / ``awarded_badge_repository``
    singletons, reset per test by ``reset_badge_repositories`` in
    ``conftest.py``.
    """

    # -- create_badge / award_badge -----------------------------------------

    def test_create_then_award_is_retrievable(self):
        # create + award round-trip: the created badge, keyed by its generated
        # id, comes back out of get_user_badges once awarded.
        user = uuid.uuid4()
        badge = badge_service.create_badge("Explorer", "desc", MinFriends(1))

        assert isinstance(badge.id, uuid.UUID)

        badge_service.award_badge(user, badge.id)

        assert badge in badge_service.get_user_badges(user)

    def test_create_badge_generates_unique_ids(self):
        first = badge_service.create_badge("Dup", None, MinFriends(1))
        second = badge_service.create_badge("Dup", None, MinFriends(1))
        assert first.id != second.id

    def test_award_records_single_award(self):
        user = uuid.uuid4()
        badge = badge_service.create_badge("First", None, MinFriends(1))

        badge_service.award_badge(user, badge.id)

        record = _award_record(user, badge.id)
        assert record is not None
        assert record.times_awarded == 1
        assert record.user_id == user
        assert record.badge_id == badge.id

    def test_award_unknown_badge_raises(self):
        with pytest.raises(ValueError):
            badge_service.award_badge(uuid.uuid4(), uuid.uuid4())

    def test_award_non_repeatable_already_held_raises(self):
        user = uuid.uuid4()
        badge = badge_service.create_badge("Once", None, MinFriends(1))
        badge_service.award_badge(user, badge.id)

        with pytest.raises(ValueError):
            badge_service.award_badge(user, badge.id)

        # The rejected re-award leaves the single existing record untouched.
        assert _award_record(user, badge.id).times_awarded == 1

    def test_award_repeatable_increments_and_refreshes(self):
        user = uuid.uuid4()
        badge = badge_service.create_badge(
            "Repeat", None, MinFriends(1), repeatable=True
        )

        badge_service.award_badge(user, badge.id)
        first_at = _award_record(user, badge.id).awarded_at
        badge_service.award_badge(user, badge.id)
        record = _award_record(user, badge.id)

        assert record.times_awarded == 2
        assert isinstance(record.awarded_at, datetime.datetime)
        assert record.awarded_at >= first_at

    def test_award_is_per_user(self):
        alice, bob = uuid.uuid4(), uuid.uuid4()
        badge = badge_service.create_badge("Shared", None, MinFriends(1))

        badge_service.award_badge(alice, badge.id)

        assert _award_record(alice, badge.id) is not None
        assert _award_record(bob, badge.id) is None

    # -- revoke_badge -------------------------------------------------------

    def test_revoke_removes_award(self):
        user = uuid.uuid4()
        badge = badge_service.create_badge("Revocable", None, MinFriends(1))
        badge_service.award_badge(user, badge.id)

        badge_service.revoke_badge(user, badge.id)

        assert _award_record(user, badge.id) is None
        assert not badge_service.has_badge(user, badge.id)

    def test_revoke_repeatable_drops_whole_record(self):
        user = uuid.uuid4()
        badge = badge_service.create_badge(
            "Repeat", None, MinFriends(1), repeatable=True
        )
        badge_service.award_badge(user, badge.id)
        badge_service.award_badge(user, badge.id)
        assert _award_record(user, badge.id).times_awarded == 2

        # A multi-count award is dropped in full, not decremented.
        badge_service.revoke_badge(user, badge.id)

        assert _award_record(user, badge.id) is None

    def test_revoke_not_held_raises(self):
        user = uuid.uuid4()
        badge = badge_service.create_badge("Unawarded", None, MinFriends(1))

        with pytest.raises(ValueError):
            badge_service.revoke_badge(user, badge.id)

    def test_revoke_unknown_badge_raises(self):
        with pytest.raises(ValueError):
            badge_service.revoke_badge(uuid.uuid4(), uuid.uuid4())

    def test_revoke_is_per_user(self):
        alice, bob = uuid.uuid4(), uuid.uuid4()
        badge = badge_service.create_badge("Shared", None, MinFriends(1))
        badge_service.award_badge(alice, badge.id)
        badge_service.award_badge(bob, badge.id)

        badge_service.revoke_badge(alice, badge.id)

        assert not badge_service.has_badge(alice, badge.id)
        assert badge_service.has_badge(bob, badge.id)

    # -- has_badge ----------------------------------------------------------

    def test_has_badge_true_and_false(self):
        user = uuid.uuid4()
        held = badge_service.create_badge("Held", None, MinFriends(1))
        unheld = badge_service.create_badge("Unheld", None, MinFriends(1))
        badge_service.award_badge(user, held.id)

        assert badge_service.has_badge(user, held.id)
        assert not badge_service.has_badge(user, unheld.id)

    def test_has_badge_times_threshold(self):
        user = uuid.uuid4()
        badge = badge_service.create_badge(
            "Repeat", None, MinFriends(1), repeatable=True
        )
        badge_service.award_badge(user, badge.id)
        badge_service.award_badge(user, badge.id)

        # Awarded exactly twice: the boundary holds, one past it fails.
        assert badge_service.has_badge(user, badge.id, times=2)
        assert not badge_service.has_badge(user, badge.id, times=3)

    def test_has_badge_unknown_badge_raises(self):
        with pytest.raises(ValueError):
            badge_service.has_badge(uuid.uuid4(), uuid.uuid4())

    def test_has_badge_times_below_one_raises(self):
        user = uuid.uuid4()
        badge = badge_service.create_badge("Held", None, MinFriends(1))
        with pytest.raises(ValueError):
            badge_service.has_badge(user, badge.id, times=0)

    def test_has_badge_times_above_one_non_repeatable_raises(self):
        user = uuid.uuid4()
        badge = badge_service.create_badge("Once", None, MinFriends(1))
        with pytest.raises(ValueError):
            badge_service.has_badge(user, badge.id, times=2)

    # -- get_user_badges ----------------------------------------------------

    def test_get_user_badges_returns_all_held(self):
        user = uuid.uuid4()
        first = badge_service.create_badge("First", None, MinFriends(1))
        second = badge_service.create_badge("Second", None, MinFriends(1))
        badge_service.award_badge(user, first.id)
        badge_service.award_badge(user, second.id)

        assert set(badge_service.get_user_badges(user)) == {first, second}

    def test_get_user_badges_empty_for_new_user(self):
        assert badge_service.get_user_badges(uuid.uuid4()) == []

    def test_get_user_badges_isolates_users(self):
        alice, bob = uuid.uuid4(), uuid.uuid4()
        alice_badge = badge_service.create_badge("Alice", None, MinFriends(1))
        bob_badge = badge_service.create_badge("Bob", None, MinFriends(1))
        badge_service.award_badge(alice, alice_badge.id)
        badge_service.award_badge(bob, bob_badge.id)

        earned = badge_service.get_user_badges(alice)

        # Bob's award never leaks into Alice's result.
        assert earned == [alice_badge]
        assert bob_badge not in earned

    def test_get_user_badges_orphan_award_raises(self):
        # An award pointing at a badge absent from the badge repository is a
        # broken invariant the getter surfaces rather than silently skips.
        user = uuid.uuid4()
        awarded_badge_repository._repository.save(
            base.AwardedBadge(user, uuid.uuid4(), datetime.datetime.now())
        )

        with pytest.raises(ValueError):
            badge_service.get_user_badges(user)

    # -- evaluate_badges ----------------------------------------------------

    def test_evaluate_awards_all_satisfied_and_returns_them(self):
        user = uuid.uuid4()
        earns_friends = badge_service.create_badge("Social", None, MinFriends(2))
        earns_events = badge_service.create_badge(
            "Attendee", None, MinEventsAttended(1)
        )
        missed = badge_service.create_badge("Popular", None, MinFriends(9))

        context = _service_context(user, events=[_event(0)], friend_count=2)
        awarded = badge_service.evaluate_badges(context)

        # Exactly the satisfied badges are returned and recorded.
        assert set(awarded) == {earns_friends, earns_events}
        assert missed not in awarded
        assert badge_service.has_badge(user, earns_friends.id)
        assert badge_service.has_badge(user, earns_events.id)
        assert not badge_service.has_badge(user, missed.id)

    def test_evaluate_does_not_award_unmet(self):
        user = uuid.uuid4()
        badge = badge_service.create_badge("Popular", None, MinFriends(5))

        awarded = badge_service.evaluate_badges(
            _service_context(user, friend_count=2)
        )

        assert awarded == []
        assert not badge_service.has_badge(user, badge.id)

    def test_evaluate_second_call_skips_non_repeatable(self):
        user = uuid.uuid4()
        badge = badge_service.create_badge("Social", None, MinFriends(2))
        context = _service_context(user, friend_count=2)

        first = badge_service.evaluate_badges(context)
        second = badge_service.evaluate_badges(context)

        assert first == [badge]
        # Already held and not repeatable: the second pass is a no-op.
        assert second == []
        assert _award_record(user, badge.id).times_awarded == 1

    def test_evaluate_repeatable_not_retriggered_without_new_activity(self):
        user = uuid.uuid4()
        badge = badge_service.create_badge(
            "Gig-goer", None, MinEventsAttended(2), repeatable=True
        )
        # ``_event`` places attendances at ``_T0`` (2026-01-01), comfortably
        # before the ``awarded_at`` wall-clock stamped by the first award.
        old = [_event(0), _event(10)]
        assert badge_service.evaluate_badges(
            _service_context(user, events=old)
        ) == [badge]

        # Re-evaluating the very same pre-award activity: ``since(awarded_at)``
        # drops all of it, so the repeatable badge is not re-triggered.
        assert badge_service.evaluate_badges(
            _service_context(user, events=old)
        ) == []
        assert _award_record(user, badge.id).times_awarded == 1

    def test_evaluate_repeatable_reawards_on_new_activity(self):
        user = uuid.uuid4()
        badge = badge_service.create_badge(
            "Gig-goer", None, MinEventsAttended(2), repeatable=True
        )
        old = [_event(0), _event(10)]
        assert badge_service.evaluate_badges(
            _service_context(user, events=old)
        ) == [badge]

        awarded_at = _award_record(user, badge.id).awarded_at
        # Two fresh attendances strictly after ``awarded_at``: ``since`` keeps
        # only these, so MinEventsAttended(2) is met over the new window.
        fresh = [
            _event_at(awarded_at + datetime.timedelta(minutes=1)),
            _event_at(awarded_at + datetime.timedelta(minutes=2)),
        ]
        second = badge_service.evaluate_badges(
            _service_context(user, events=old + fresh)
        )

        assert second == [badge]
        assert _award_record(user, badge.id).times_awarded == 2

    def test_evaluate_no_badges_returns_empty(self):
        assert badge_service.evaluate_badges(
            _service_context(uuid.uuid4())
        ) == []