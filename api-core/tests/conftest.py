# api-core/tests/conftest.py
"""Pytest configuration and shared fixtures for the api-core test suite.

The api-core modules use package-qualified intra-package imports (e.g.
``import friends.base as base``, ``import friends.friendship_repository``), so
the ``src`` source root must be on ``sys.path`` for the suite to resolve them.
This file adds it so the suite runs from the repo root with
``python -m pytest api-core/tests/``.

It also resets the module-level repository singletons before every test: the
service layers operate on module-level ``_repository`` instances (the friends
and attendance slices), and without a reset that state would leak between tests.
"""

import os
import sys

import pytest

_SRC = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "src"))

if _SRC not in sys.path:
    sys.path.insert(0, _SRC)


# Canned event info the stubbed bridge serves; the values must parse the way the
# badges evaluation code expects (``host`` a UUID, ``start`` ISO-8601, ``category``
# a string), and match the canned data in ``SubprocessResponder.java``.
_CANNED_EVENT_INFO = {
    "host": "22222222-2222-2222-2222-222222222222",
    "start": "2026-09-15T18:00:00",
    "category": "SOCIAL",
}


@pytest.fixture(autouse=True)
def mock_bridge(request, monkeypatch):
    """Stub the subprocess bridge so no unit test spawns a real responder.

    Several service paths reach across the bridge transitively: recording an
    attendance or accepting a friend request publishes an ``activity`` event,
    which the badges ``evaluation_listener`` answers by calling
    ``bridge.get_event_info`` for each attended event; ``recommendations`` calls
    ``bridge.get_user_events``. Left unmocked, those spawn the real Java
    responder, which only recognises a few canned UUIDs and raises
    ``SubprocessError`` for the random ids these tests use. Patching both bridge
    entry points to return canned data keeps the unit suite free of the
    JDK/subprocess dependency.

    Tests marked ``integration`` opt out to exercise the real round trip; a test
    wanting bespoke bridge behaviour (e.g. ``test_recommendations``'s ``world``)
    simply re-patches these names on top.
    """

    if request.node.get_closest_marker("integration"):
        yield
        return

    import bridge

    monkeypatch.setattr(
        bridge, "get_event_info", lambda event_id: dict(_CANNED_EVENT_INFO)
    )
    monkeypatch.setattr(bridge, "get_user_events", lambda user_id: [])
    yield


@pytest.fixture(autouse=True)
def reset_repository():
    """Give every test a clean, isolated friends repository.

    The service layer reads ``friendship_repository._repository`` at call time,
    so replacing the singleton here is picked up by all service functions.
    """

    import friends.friendship_repository as friendship_repository

    friendship_repository._repository = (
        friendship_repository.InMemoryFriendshipRepository()
    )
    yield


@pytest.fixture(autouse=True)
def reset_badge_repositories():
    """Give every test clean, isolated badge repositories.

    ``badge_service`` reads the module-level ``_repository`` singletons of both
    ``badge_repository`` and ``awarded_badge_repository`` at call time, so
    swapping them here isolates badge and award state between tests (mirrors the
    ``reset_repository`` pattern for the friends slice).
    """

    import badges.awarded_badge_repository as awarded_badge_repository
    import badges.badge_repository as badge_repository

    badge_repository._repository = badge_repository.InMemoryBadgeRepository()
    awarded_badge_repository._repository = (
        awarded_badge_repository.InMemoryAwardedBadgeRepository()
    )
    yield


@pytest.fixture(autouse=True)
def reset_activity_listeners():
    """Give every test a clean ``activity`` listener registry.

    The registry is a process-wide, module-level set, so without a reset the
    listeners one test subscribes would leak into the next. After clearing, the
    badge ``evaluation`` listener is re-registered -- the subscription
    ``badges`` installs on import -- so automatic badge evaluation stays wired
    for the integration tests without leaking any test-local spies. Cleared
    again on teardown so nothing survives into an unrelated test.
    """

    import activity.base
    from badges import evaluation

    # ``__listeners`` is module-private with no public reset hook; reaching in
    # here (outside any class, so unmangled) is the reset seam for the tests.
    activity.base.__listeners.clear()
    evaluation.register()
    yield
    activity.base.__listeners.clear()
    
@pytest.fixture(autouse=True)
def reset_attendance_repository():
    """Give every test a clean, isolated attendance repository.

    The service layer reads ``attendance_repository._repository`` at call time,
    so replacing the singleton here is picked up by all service functions.
    """

    import attendance.attendance_repository as attendance_repository

    attendance_repository._repository = (
        attendance_repository.InMemoryAttendanceRepository()
    )
    yield
