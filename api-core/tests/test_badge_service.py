"""Tests for the ``badges.badge_service`` service-layer operations.

Where ``test_badges.py`` pins the predicate vocabulary and
``test_evaluation.py`` pins the automatic ``activity`` -> evaluation wiring,
this module exercises the service functions directly, against the live
module-level repository singletons:

  * ``TestCreateBadge``      -- persistence, defaults and id uniqueness.
  * ``TestAwardBadge``       -- first award, non-repeatable rejection, and the
                                repeatable increment / timestamp refresh.
  * ``TestRevokeBadge``      -- record removal and the two ``ValueError`` paths.
  * ``TestHasBadge``         -- the ``times`` threshold and its validation.
  * ``TestGetUserBadges``    -- per-user filtering and the orphan-award guard.
  * ``TestEvaluateBadges``   -- direct evaluation: awarding, the non-repeatable
                                skip, repeatable re-award, and the return value.

The badge repositories are reset per test by the ``reset_badge_repositories``
autouse fixture in ``conftest.py``; the subprocess bridge is stubbed by
``mock_bridge`` so nothing here spawns a JVM.

Run from the repo root:  ``python -m pytest api-core/tests/``
"""

import datetime
import uuid

import pytest

import badges.awarded_badge_repository as awarded_badge_repository
import badges.badge_repository as badge_repository
import badges.base as base
from badges import badge_service
from badges.predicates import (
    AttendedEvent,
    AwardContext,
    MinEventsAttended,
    MinFriends,
)

_HOST = uuid.UUID("11111111-1111-1111-1111-111111111111")
_T0 = datetime.datetime(2026, 1, 1, 12, 0)


def _award_record(user_id, badge_id):
    """Return the stored ``AwardedBadge`` for a pair, or ``None``."""

    return awarded_badge_repository._repository.find_by_id(
        base._generate_award_id(user_id, badge_id)
    )


def _event(offset_minutes=0, host_id=_HOST, categories=("music",)):
    """Build an ``AttendedEvent`` ``offset_minutes`` after ``_T0``."""

    return AttendedEvent(
        event_id=uuid.uuid4(),
        host_id=host_id,
        categories=frozenset(categories),
        start_time=_T0 + datetime.timedelta(minutes=offset_minutes),
    )


def _context(user_id, events=(), friend_count=0):
    """Build an ``AwardContext`` for ``user_id`` from event objects."""

    return AwardContext(
        user_id=user_id,
        attended_events=tuple(events),
        friend_count=friend_count,
    )


class TestCreateBadge:
    """``create_badge`` builds a ``Badge`` and persists it to the repository."""

    def test_returns_badge_with_supplied_fields(self):
        condition = MinFriends(3)
        badge = badge_service.create_badge(
            "Social", "makes friends", condition, repeatable=True
        )

        assert badge.name == "Social"
        assert badge.description == "makes friends"
        assert badge.award_condition == condition
        assert badge.repeatable is True
        assert isinstance(badge.id, uuid.UUID)

    def test_persists_to_the_badge_repository(self):
        badge = badge_service.create_badge("Persisted", None, MinFriends(1))

        assert badge_repository._repository.find_by_id(badge.id) == badge

    def test_description_is_optional(self):
        badge = badge_service.create_badge("No description", None, MinFriends(1))
        assert badge.description is None

    def test_repeatable_defaults_to_false(self):
        badge = badge_service.create_badge("One shot", None, MinFriends(1))
        assert badge.repeatable is False

    def test_ids_are_unique_across_calls(self):
        first = badge_service.create_badge("A", None, MinFriends(1))
        second = badge_service.create_badge("A", None, MinFriends(1))
        # Same name, but two distinct persisted records.
        assert first.id != second.id
        assert len(badge_repository._repository.find_all()) == 2


class TestAwardBadge:
    """``award_badge`` records a manual award and governs re-awards."""

    def test_first_award_creates_record_with_one_count(self):
        user = uuid.uuid4()
        badge = badge_service.create_badge("First", None, MinFriends(1))

        badge_service.award_badge(user, badge.id)

        record = _award_record(user, badge.id)
        assert record is not None
        assert record.times_awarded == 1
        assert record.user_id == user
        assert record.badge_id == badge.id

    def test_unknown_badge_raises(self):
        with pytest.raises(ValueError):
            badge_service.award_badge(uuid.uuid4(), uuid.uuid4())

    def test_second_award_of_non_repeatable_raises(self):
        user = uuid.uuid4()
        badge = badge_service.create_badge("Once", None, MinFriends(1))
        badge_service.award_badge(user, badge.id)

        with pytest.raises(ValueError):
            badge_service.award_badge(user, badge.id)

        # The rejection leaves the original single award untouched.
        assert _award_record(user, badge.id).times_awarded == 1

    def test_repeatable_award_increments_count(self):
        user = uuid.uuid4()
        badge = badge_service.create_badge(
            "Repeat", None, MinFriends(1), repeatable=True
        )

        badge_service.award_badge(user, badge.id)
        badge_service.award_badge(user, badge.id)
        badge_service.award_badge(user, badge.id)

        assert _award_record(user, badge.id).times_awarded == 3

    def test_repeatable_reaward_refreshes_timestamp(self):
        user = uuid.uuid4()
        badge = badge_service.create_badge(
            "Repeat", None, MinFriends(1), repeatable=True
        )

        badge_service.award_badge(user, badge.id)
        first_awarded_at = _award_record(user, badge.id).awarded_at
        badge_service.award_badge(user, badge.id)
        second_awarded_at = _award_record(user, badge.id).awarded_at

        assert isinstance(second_awarded_at, datetime.datetime)
        assert second_awarded_at >= first_awarded_at

    def test_awards_to_different_users_are_independent(self):
        alice, bob = uuid.uuid4(), uuid.uuid4()
        badge = badge_service.create_badge("Shared", None, MinFriends(1))

        badge_service.award_badge(alice, badge.id)

        assert _award_record(alice, badge.id) is not None
        assert _award_record(bob, badge.id) is None


class TestRevokeBadge:
    """``revoke_badge`` removes an award record entirely."""

    def test_revoke_removes_the_award(self):
        user = uuid.uuid4()
        badge = badge_service.create_badge("Revocable", None, MinFriends(1))
        badge_service.award_badge(user, badge.id)

        badge_service.revoke_badge(user, badge.id)

        assert _award_record(user, badge.id) is None
        assert not badge_service.has_badge(user, badge.id)

    def test_revoke_unknown_badge_raises(self):
        with pytest.raises(ValueError):
            badge_service.revoke_badge(uuid.uuid4(), uuid.uuid4())

    def test_revoke_when_not_awarded_raises(self):
        user = uuid.uuid4()
        badge = badge_service.create_badge("Unawarded", None, MinFriends(1))

        with pytest.raises(ValueError):
            badge_service.revoke_badge(user, badge.id)

    def test_revoke_removes_repeatable_award_in_full(self):
        user = uuid.uuid4()
        badge = badge_service.create_badge(
            "Repeat", None, MinFriends(1), repeatable=True
        )
        badge_service.award_badge(user, badge.id)
        badge_service.award_badge(user, badge.id)
        assert _award_record(user, badge.id).times_awarded == 2

        # A multi-count award is dropped wholesale, not decremented.
        badge_service.revoke_badge(user, badge.id)

        assert _award_record(user, badge.id) is None

    def test_revoke_only_affects_the_targeted_user(self):
        alice, bob = uuid.uuid4(), uuid.uuid4()
        badge = badge_service.create_badge("Shared", None, MinFriends(1))
        badge_service.award_badge(alice, badge.id)
        badge_service.award_badge(bob, badge.id)

        badge_service.revoke_badge(alice, badge.id)

        assert not badge_service.has_badge(alice, badge.id)
        assert badge_service.has_badge(bob, badge.id)


class TestHasBadge:
    """``has_badge`` reports whether a user meets a ``times`` threshold."""

    def test_false_when_never_awarded(self):
        user = uuid.uuid4()
        badge = badge_service.create_badge("Unheld", None, MinFriends(1))
        assert not badge_service.has_badge(user, badge.id)

    def test_true_when_awarded_once(self):
        user = uuid.uuid4()
        badge = badge_service.create_badge("Held", None, MinFriends(1))
        badge_service.award_badge(user, badge.id)
        assert badge_service.has_badge(user, badge.id)

    def test_times_threshold_against_repeatable_award(self):
        user = uuid.uuid4()
        badge = badge_service.create_badge(
            "Repeat", None, MinFriends(1), repeatable=True
        )
        badge_service.award_badge(user, badge.id)
        badge_service.award_badge(user, badge.id)

        # Awarded exactly twice: the boundary passes, one past it fails.
        assert badge_service.has_badge(user, badge.id, times=2)
        assert not badge_service.has_badge(user, badge.id, times=3)

    def test_unknown_badge_raises(self):
        with pytest.raises(ValueError):
            badge_service.has_badge(uuid.uuid4(), uuid.uuid4())

    def test_times_below_one_raises(self):
        user = uuid.uuid4()
        badge = badge_service.create_badge("Held", None, MinFriends(1))
        with pytest.raises(ValueError):
            badge_service.has_badge(user, badge.id, times=0)

    def test_times_above_one_on_non_repeatable_raises(self):
        user = uuid.uuid4()
        badge = badge_service.create_badge("Once", None, MinFriends(1))
        with pytest.raises(ValueError):
            badge_service.has_badge(user, badge.id, times=2)


class TestGetUserBadges:
    """``get_user_badges`` returns the badges a single user has earned."""

    def test_empty_when_user_has_no_awards(self):
        assert badge_service.get_user_badges(uuid.uuid4()) == []

    def test_returns_only_the_users_badges(self):
        alice, bob = uuid.uuid4(), uuid.uuid4()
        first = badge_service.create_badge("First", None, MinFriends(1))
        second = badge_service.create_badge("Second", None, MinFriends(1))
        other = badge_service.create_badge("Other", None, MinFriends(1))

        badge_service.award_badge(alice, first.id)
        badge_service.award_badge(alice, second.id)
        badge_service.award_badge(bob, other.id)

        earned = badge_service.get_user_badges(alice)

        assert set(earned) == {first, second}
        assert other not in earned

    def test_orphaned_award_raises(self):
        # An award pointing at a badge absent from the badge repository is a
        # broken invariant the getter surfaces rather than silently skips.
        user = uuid.uuid4()
        missing_badge_id = uuid.uuid4()
        awarded_badge_repository._repository.save(
            base.AwardedBadge(user, missing_badge_id, datetime.datetime.now())
        )

        with pytest.raises(ValueError):
            badge_service.get_user_badges(user)


class TestEvaluateBadges:
    """``evaluate_badges`` auto-awards from an ``AwardContext`` snapshot."""

    def test_awards_when_condition_is_satisfied(self):
        user = uuid.uuid4()
        badge = badge_service.create_badge("Social", None, MinFriends(2))

        awarded = badge_service.evaluate_badges(_context(user, friend_count=2))

        assert awarded == [badge]
        assert badge_service.has_badge(user, badge.id)

    def test_does_not_award_when_condition_unsatisfied(self):
        user = uuid.uuid4()
        badge = badge_service.create_badge("Popular", None, MinFriends(5))

        awarded = badge_service.evaluate_badges(_context(user, friend_count=2))

        assert awarded == []
        assert not badge_service.has_badge(user, badge.id)

    def test_only_satisfied_badges_are_awarded(self):
        user = uuid.uuid4()
        earns = badge_service.create_badge("Earns", None, MinFriends(2))
        missed = badge_service.create_badge("Missed", None, MinFriends(9))

        awarded = badge_service.evaluate_badges(_context(user, friend_count=2))

        assert awarded == [earns]
        assert badge_service.has_badge(user, earns.id)
        assert not badge_service.has_badge(user, missed.id)

    def test_awards_a_time_bounded_event_badge(self):
        user = uuid.uuid4()
        badge = badge_service.create_badge(
            "Regular", None, MinEventsAttended(3)
        )
        context = _context(user, events=[_event(i * 10) for i in range(3)])

        awarded = badge_service.evaluate_badges(context)

        assert awarded == [badge]

    def test_non_repeatable_held_badge_is_skipped(self):
        user = uuid.uuid4()
        badge = badge_service.create_badge("Social", None, MinFriends(2))
        context = _context(user, friend_count=2)

        first = badge_service.evaluate_badges(context)
        second = badge_service.evaluate_badges(context)

        assert first == [badge]
        # Already held and not repeatable, so a second pass awards nothing and
        # leaves the single award untouched.
        assert second == []
        assert _award_record(user, badge.id).times_awarded == 1

    def test_repeatable_badge_is_reawarded(self):
        user = uuid.uuid4()
        # ``friend_count`` is a state fact that passes through the re-award
        # window unchanged, so a repeatable friend badge re-satisfies each pass.
        badge = badge_service.create_badge(
            "Social", None, MinFriends(2), repeatable=True
        )
        context = _context(user, friend_count=2)

        first = badge_service.evaluate_badges(context)
        second = badge_service.evaluate_badges(context)

        assert first == [badge]
        assert second == [badge]
        assert _award_record(user, badge.id).times_awarded == 2

    def test_no_badges_defined_returns_empty(self):
        assert badge_service.evaluate_badges(_context(uuid.uuid4())) == []
