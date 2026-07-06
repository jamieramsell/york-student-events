"""Unit tests for the badges slice.

Currently covers ``predicates`` serialisation:

  * ``TestPredicateSerialisation``
        ``to_dict`` shape for a leaf and a nested composite, the non-trivial
        scalar conversions (UUID / datetime / timedelta / ``between``), the
        ``to_dict`` -> ``predicate_from_dict`` round-trip for every predicate
        type (including through a real JSON dump/load), and the ``ValueError``
        raised on an unknown or missing type tag.

Run from the repo root:  ``python -m pytest api-core/tests/``
"""

import datetime
import json
import uuid

import pytest

from badges.predicates import (
    AndPredicate,
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
_START = datetime.datetime(2026, 10, 31, 20, 0)
_END = datetime.datetime(2026, 11, 1, 6, 0)

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