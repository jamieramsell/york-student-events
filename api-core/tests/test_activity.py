"""Unit tests for the ``activity`` publish/subscribe registry (issue #156).

``activity`` is pure plumbing: a synchronous, payload-minimal fan-in that lets a
fact-producing slice (e.g. ``friends``) signal "this user's facts changed"
without importing a consuming slice (e.g. ``badges``) directly. These tests pin
that contract in isolation:

  * ``TestPublicSurface``
        the package re-exports ``subscribe`` / ``publish`` (mirrors the
        ``friends`` / ``badges`` package convention).
  * ``TestPublish``
        ``publish`` is a no-op with no subscribers, delivers the exact
        ``user_id`` to a subscriber, fans out to every subscriber, and fires
        once per ``publish`` call.
  * ``TestExceptionPropagation``
        a listener raising propagates out of ``publish`` (the repo's fail-loud
        convention -- no silent swallowing).
  * ``TestSubscribe``
        re-subscribing the same callable does not register a duplicate.

Run from the repo root:  ``python -m pytest api-core/tests/``
"""

import uuid

import activity
import activity.base
import pytest


@pytest.fixture
def clean_registry(reset_activity_listeners):
    """Yield a registry with no listeners at all.

    Depends on the autouse ``reset_activity_listeners`` fixture so it is set up
    *after* it, then strips the badge ``evaluation`` listener that fixture
    re-registers. This gives the registry's own unit tests a pristine slate,
    where "no subscribers" really means zero.
    """

    activity.base.__listeners.clear()
    yield


def _spy():
    """Return a listener that records every ``user_id`` it is published."""

    received: list[uuid.UUID] = []

    def listener(user_id: uuid.UUID) -> None:
        received.append(user_id)

    listener.received = received
    return listener


# ---------------------------------------------------------------------------
# Public surface
# ---------------------------------------------------------------------------
class TestPublicSurface:
    def test_reexports_subscribe_and_publish(self):
        assert callable(activity.subscribe)
        assert callable(activity.publish)

    def test_all_declares_the_public_surface(self):
        assert set(activity.__all__) == {"subscribe", "publish"}


# ---------------------------------------------------------------------------
# publish
# ---------------------------------------------------------------------------
class TestPublish:
    def test_publish_with_no_subscribers_is_a_noop(self, clean_registry):
        # Must not raise despite there being nothing to notify.
        activity.publish(uuid.uuid4())

    def test_subscribed_listener_receives_the_published_id(self, clean_registry):
        spy = _spy()
        activity.subscribe(spy)

        user_id = uuid.uuid4()
        activity.publish(user_id)

        assert spy.received == [user_id]

    def test_publish_passes_the_exact_id_object(self, clean_registry):
        spy = _spy()
        activity.subscribe(spy)

        user_id = uuid.uuid4()
        activity.publish(user_id)

        # The identical object is forwarded, not a copy or a stringified form.
        assert spy.received[0] is user_id

    def test_all_subscribers_are_invoked(self, clean_registry):
        # The registry is a set, so delivery order is unspecified; every
        # subscriber must nonetheless be notified exactly once.
        spies = [_spy() for _ in range(3)]
        for spy in spies:
            activity.subscribe(spy)

        user_id = uuid.uuid4()
        activity.publish(user_id)

        for spy in spies:
            assert spy.received == [user_id]

    def test_each_publish_fires_the_listeners_again(self, clean_registry):
        spy = _spy()
        activity.subscribe(spy)

        first, second = uuid.uuid4(), uuid.uuid4()
        activity.publish(first)
        activity.publish(second)

        assert spy.received == [first, second]

    def test_publish_returns_none(self, clean_registry):
        activity.subscribe(_spy())
        assert activity.publish(uuid.uuid4()) is None


# ---------------------------------------------------------------------------
# Exception propagation
# ---------------------------------------------------------------------------
class TestExceptionPropagation:
    def test_listener_exception_propagates_out_of_publish(self, clean_registry):
        def boom(user_id: uuid.UUID) -> None:
            raise ValueError("listener failure")

        activity.subscribe(boom)

        # Fail-loud: the publisher sees the error rather than it being swallowed.
        with pytest.raises(ValueError, match="listener failure"):
            activity.publish(uuid.uuid4())


# ---------------------------------------------------------------------------
# subscribe
# ---------------------------------------------------------------------------
class TestSubscribe:
    def test_subscribe_returns_none(self, clean_registry):
        assert activity.subscribe(_spy()) is None

    def test_resubscribing_same_listener_does_not_duplicate(self, clean_registry):
        spy = _spy()
        activity.subscribe(spy)
        activity.subscribe(spy)

        # Registered once despite two subscribe calls, so a single delivery.
        activity.publish(uuid.uuid4())
        assert len(spy.received) == 1
