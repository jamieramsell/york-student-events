"""Tests for automatic badge evaluation wiring (issue #158).

``badges/evaluation.py`` closes the loop between the ``friends`` publisher
(#157) and the ``badges`` slice: it builds an ``AwardContext`` for a user and
re-evaluates their badges whenever ``activity`` publishes for them. Layout:

  * ``TestBuildAwardContext``
        unit tests for ``build_award_context`` -- friend count is read from the
        friends slice; ``attended_events`` / ``messages_sent`` are documented
        empty stubs.
  * ``TestRegister``
        ``register`` subscribes the listener and is idempotent (no duplicate
        subscription).
  * ``TestAutomaticAward``
        end-to-end through the injected services: accepting friend requests
        drives awards with no explicit ``evaluate_badges`` call, and re-firing
        on unchanged / further activity never double-awards a non-repeatable
        badge.

These run through a fresh, consistently wired service graph (the friends, badge
and evaluation services and the ``activity`` registry), built per test by the
autouse fixtures in ``conftest.py``.

Run from the repo root:  ``python -m pytest api-core/tests/``
"""

import uuid

import activity
import activity.base
import badges.base as badges_base
import bridge
from attendance import AttendanceService, InMemoryAttendanceRepository
from badges import (
    BadgeService,
    EvaluationService,
    InMemoryAwardedBadgeRepository,
    InMemoryBadgeRepository,
)
from badges.predicates import MinFriends
from friends import FriendshipService, InMemoryFriendshipRepository

# Module-level defaults so the file is usable on its own; ``compose_services`` in
# conftest.py swaps in a single, consistently wired graph (the same
# ``friendship_service`` and ``badge_service`` the ``evaluation_service`` reads)
# before every test.
friendship_service = FriendshipService(InMemoryFriendshipRepository())
awarded_badge_repo = InMemoryAwardedBadgeRepository()
badge_service = BadgeService(InMemoryBadgeRepository(), awarded_badge_repo)
evaluation_service = EvaluationService(
    AttendanceService(InMemoryAttendanceRepository()),
    friendship_service,
    badge_service,
)


def _befriend(user_id: uuid.UUID, friend_id: uuid.UUID) -> None:
    """Establish an accepted friendship through the live service."""

    friendship_service.send_friend_request(user_id, friend_id)
    friendship_service.accept_friend_request(user_id, friend_id)


def _award(user_id: uuid.UUID, badge_id: uuid.UUID):
    """Return the stored ``AwardedBadge`` record, or ``None`` if unawarded."""

    award_id = badges_base._generate_award_id(user_id, badge_id)
    return awarded_badge_repo.find_by_id(award_id)


def _listener_registry() -> set:
    """Return the ``activity`` listener set.

    ``activity.base.__listeners`` has no public accessor, and referencing it
    literally inside a test *class* would be name-mangled to
    ``_ClassName__listeners``; fetching it by string via ``getattr`` sidesteps
    that so the register tests can inspect the subscription.
    """

    return getattr(activity.base, "__listeners")


# ---------------------------------------------------------------------------
# build_award_context
# ---------------------------------------------------------------------------
class TestBuildAwardContext:
    def test_friend_count_is_read_from_the_friends_slice(self):
        a, b, c = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        _befriend(a, b)
        _befriend(a, c)

        context = evaluation_service.build_award_context(a)

        assert context.friend_count == 2

    def test_zero_friends_gives_zero_count(self):
        context = evaluation_service.build_award_context(uuid.uuid4())
        assert context.friend_count == 0

    def test_attended_events_and_messages_are_empty_stubs(self):
        # Both fact sources are documented stubs until their slices land; the
        # context must still be well-formed empty tuples, not ``None``.
        context = evaluation_service.build_award_context(uuid.uuid4())
        assert context.attended_events == ()
        assert context.messages_sent == ()

    def test_context_carries_the_requested_user_id(self):
        user_id = uuid.uuid4()
        assert evaluation_service.build_award_context(user_id).user_id == user_id


# ---------------------------------------------------------------------------
# register
# ---------------------------------------------------------------------------
class TestRegister:
    def test_register_subscribes_the_evaluation_listener(self):
        _listener_registry().clear()
        evaluation_service.register()
        assert evaluation_service.evaluation_listener in _listener_registry()

    def test_register_is_idempotent(self):
        # Calling register() more than once must not register a duplicate
        # listener (the subscription is a set of the same bound method, which
        # compares equal across calls on the same instance).
        _listener_registry().clear()
        evaluation_service.register()
        evaluation_service.register()
        assert len(_listener_registry()) == 1


# ---------------------------------------------------------------------------
# Automatic award through the injected services
# ---------------------------------------------------------------------------
class TestAutomaticAward:
    def test_reaching_threshold_awards_badge_without_explicit_evaluate(self):
        badge = badge_service.create_badge("Social Butterfly", None, MinFriends(2))
        a, b, c = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()

        _befriend(a, b)
        # One friend is below the threshold, so nothing is awarded yet.
        assert not badge_service.has_badge(a, badge.id)

        _befriend(a, c)
        # Crossing to two friends auto-awards purely via the accept -> publish
        # -> evaluation chain; note there is no evaluate_badges call anywhere.
        assert badge_service.has_badge(a, badge.id)

    def test_threshold_not_reached_is_not_awarded(self):
        badge = badge_service.create_badge("Very Popular", None, MinFriends(5))
        a, b, c = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()

        _befriend(a, b)
        _befriend(a, c)

        assert not badge_service.has_badge(a, badge.id)

    def test_republishing_unchanged_facts_does_not_double_award(self):
        badge = badge_service.create_badge("Social Butterfly", None, MinFriends(2))
        a, b, c = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        _befriend(a, b)
        _befriend(a, c)
        assert badge_service.has_badge(a, badge.id)

        # Publish again directly with the same facts. (Re-accepting would raise
        # before any publish, so it would never exercise this path.)
        activity.publish(a)

        assert badge_service.has_badge(a, badge.id)
        assert _award(a, badge.id).times_awarded == 1

    def test_gaining_a_friend_beyond_threshold_leaves_award_unchanged(self):
        badge = badge_service.create_badge("Social Butterfly", None, MinFriends(2))
        a, b, c, d = uuid.uuid4(), uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        _befriend(a, b)
        _befriend(a, c)
        assert _award(a, badge.id).times_awarded == 1

        # A third friend re-fires evaluation, but the non-repeatable badge is
        # already held, so it is skipped -- not re-awarded.
        _befriend(a, d)

        assert _award(a, badge.id).times_awarded == 1


# ---------------------------------------------------------------------------
# event-service notification on auto-award (issue #169)
# ---------------------------------------------------------------------------
class TestBadgeAwardNotification:
    """The listener must notify event-service over the bridge for every badge it
    auto-awards, while never letting a bridge failure escape back to the
    publisher that triggered evaluation. ``bridge.notify_badge_awarded`` is
    stubbed per test (over the no-op the conftest installs) so no JVM is spawned.
    """

    def test_notifies_event_service_for_each_awarded_badge(self, monkeypatch):
        notified = []
        monkeypatch.setattr(
            bridge,
            "notify_badge_awarded",
            lambda user_id, badge_name: notified.append((user_id, badge_name)),
        )
        badge_service.create_badge("Social Butterfly", None, MinFriends(2))
        a, b, c = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()

        _befriend(a, b)
        _befriend(a, c)  # crossing to two friends auto-awards -> notifies

        assert (a, "Social Butterfly") in notified

    def test_does_not_notify_when_nothing_is_awarded(self, monkeypatch):
        notified = []
        monkeypatch.setattr(
            bridge,
            "notify_badge_awarded",
            lambda user_id, badge_name: notified.append((user_id, badge_name)),
        )
        badge_service.create_badge("Very Popular", None, MinFriends(5))
        a, b = uuid.uuid4(), uuid.uuid4()

        _befriend(a, b)  # one friend is below the threshold

        assert notified == []

    def test_bridge_failure_does_not_propagate_or_undo_the_award(self, monkeypatch):
        def boom(user_id, badge_name):
            raise bridge.SubprocessError("event-service unavailable")

        monkeypatch.setattr(bridge, "notify_badge_awarded", boom)
        badge = badge_service.create_badge("Social Butterfly", None, MinFriends(2))
        a, b, c = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()

        _befriend(a, b)
        # The award fires the failing notification; accepting the request must
        # still succeed (no exception) and the badge must remain awarded.
        _befriend(a, c)

        assert badge_service.has_badge(a, badge.id)
