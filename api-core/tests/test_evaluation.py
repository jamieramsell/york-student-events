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
        end-to-end through the live singletons: accepting friend requests
        drives awards with no explicit ``evaluate_badges`` call, and re-firing
        on unchanged / further activity never double-awards a non-repeatable
        badge.

These run through the real module-level singletons (the friends repository, the
badge repositories and the ``activity`` registry), all reset per test by the
autouse fixtures in ``conftest.py``.

Run from the repo root:  ``python -m pytest api-core/tests/``
"""

import uuid

import activity
import activity.base
import badges
import badges.awarded_badge_repository as awarded_badge_repository
import badges.base as badges_base
import friendship_service
from badges import evaluation
from badges.predicates import MinFriends


def _befriend(user_id: uuid.UUID, friend_id: uuid.UUID) -> None:
    """Establish an accepted friendship through the live service."""

    friendship_service.send_friend_request(user_id, friend_id)
    friendship_service.accept_friend_request(user_id, friend_id)


def _award(user_id: uuid.UUID, badge_id: uuid.UUID):
    """Return the stored ``AwardedBadge`` record, or ``None`` if unawarded."""

    award_id = badges_base._generate_award_id(user_id, badge_id)
    return awarded_badge_repository._repository.find_by_id(award_id)


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

        context = evaluation.build_award_context(a)

        assert context.friend_count == 2

    def test_zero_friends_gives_zero_count(self):
        context = evaluation.build_award_context(uuid.uuid4())
        assert context.friend_count == 0

    def test_attended_events_and_messages_are_empty_stubs(self):
        # Both fact sources are documented stubs until their slices land; the
        # context must still be well-formed empty tuples, not ``None``.
        context = evaluation.build_award_context(uuid.uuid4())
        assert context.attended_events == ()
        assert context.messages_sent == ()

    def test_context_carries_the_requested_user_id(self):
        user_id = uuid.uuid4()
        assert evaluation.build_award_context(user_id).user_id == user_id


# ---------------------------------------------------------------------------
# register
# ---------------------------------------------------------------------------
class TestRegister:
    def test_register_subscribes_the_evaluation_listener(self):
        _listener_registry().clear()
        evaluation.register()
        assert evaluation.evaluation_listener in _listener_registry()

    def test_register_is_idempotent(self):
        # Calling register() more than once must not register a duplicate
        # listener (the subscription is a set of the same module-level callable).
        _listener_registry().clear()
        evaluation.register()
        evaluation.register()
        assert len(_listener_registry()) == 1


# ---------------------------------------------------------------------------
# Automatic award through the live singletons
# ---------------------------------------------------------------------------
class TestAutomaticAward:
    def test_reaching_threshold_awards_badge_without_explicit_evaluate(self):
        badge = badges.create_badge("Social Butterfly", None, MinFriends(2))
        a, b, c = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()

        _befriend(a, b)
        # One friend is below the threshold, so nothing is awarded yet.
        assert not badges.has_badge(a, badge.id)

        _befriend(a, c)
        # Crossing to two friends auto-awards purely via the accept -> publish
        # -> evaluation chain; note there is no evaluate_badges call anywhere.
        assert badges.has_badge(a, badge.id)

    def test_threshold_not_reached_is_not_awarded(self):
        badge = badges.create_badge("Very Popular", None, MinFriends(5))
        a, b, c = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()

        _befriend(a, b)
        _befriend(a, c)

        assert not badges.has_badge(a, badge.id)

    def test_republishing_unchanged_facts_does_not_double_award(self):
        badge = badges.create_badge("Social Butterfly", None, MinFriends(2))
        a, b, c = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        _befriend(a, b)
        _befriend(a, c)
        assert badges.has_badge(a, badge.id)

        # Publish again directly with the same facts. (Re-accepting would raise
        # before any publish, so it would never exercise this path.)
        activity.publish(a)

        assert badges.has_badge(a, badge.id)
        assert _award(a, badge.id).times_awarded == 1

    def test_gaining_a_friend_beyond_threshold_leaves_award_unchanged(self):
        badge = badges.create_badge("Social Butterfly", None, MinFriends(2))
        a, b, c, d = uuid.uuid4(), uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        _befriend(a, b)
        _befriend(a, c)
        assert _award(a, badge.id).times_awarded == 1

        # A third friend re-fires evaluation, but the non-repeatable badge is
        # already held, so it is skipped -- not re-awarded.
        _befriend(a, d)

        assert _award(a, badge.id).times_awarded == 1
