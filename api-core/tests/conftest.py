# api-core/tests/conftest.py
"""Pytest configuration and shared fixtures for the api-core test suite.

The api-core modules use package-qualified intra-package imports (e.g.
``import friends.base as base``, ``import friends.friendship_repository``), so
the ``src`` source root must be on ``sys.path`` for the suite to resolve them.
This file adds it so the suite runs from the repo root with
``python -m pytest api-core/tests/``.

It also acts as the test suite's composition root: the service layers now take
their repositories (and collaborating services) via constructor injection, so
``compose_services`` builds a fresh, isolated service graph before every test and
publishes the instances onto the test modules that drive them. No module-level
repository singletons survive between tests, so state cannot leak.
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
    ``bridge.get_event_info`` for each attended event and ``notify_badge_awarded``
    for each newly awarded badge; ``recommendations`` calls
    ``bridge.get_user_events``. Left unmocked, those spawn the real Java
    responder, which only recognises a few canned UUIDs and raises
    ``SubprocessError`` for the random ids these tests use. Patching every bridge
    entry point keeps the unit suite free of the JDK/subprocess dependency.

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
    monkeypatch.setattr(
        bridge, "notify_badge_awarded", lambda user_id, badge_name: None
    )
    yield


@pytest.fixture(autouse=True)
def compose_services():
    """Build a fresh, isolated service graph and expose it to the test modules.

    This is the test suite's composition root. Rather than assembling the graph
    by hand, it calls the shared ``bootstrap`` with bare repositories (so the
    unit suite starts from empty state) and ``register=False`` (the ``activity``
    registry is wired per test by ``reset_activity_listeners`` below), then
    publishes the constructed repositories and services onto the module globals
    the tests read at call time. Because a new graph is built every test, no
    attendance/friend/badge state leaks between tests.
    """

    import test_attendance
    import test_badges
    import test_evaluation
    import test_friends
    import test_recommendations
    import test_recommendations_integration

    import bootstrap

    # Fresh graph, over bare repositories (never the canned variants) so the
    # unit suite starts from empty state.
    services = bootstrap.bootstrap(register=False)

    # Publish the instances (and the underlying repositories the tests assert
    # against) onto each test module's globals.
    test_attendance.attendance_service = services.attendance_service
    test_attendance.attendance_repo = services.attendance_repo

    test_friends.friendship_service = services.friendship_service
    test_friends.friendship_repo = services.friendship_repo

    test_badges.badge_service = services.badge_service
    test_badges.badge_repo = services.badge_repo
    test_badges.awarded_badge_repo = services.awarded_badge_repo

    test_evaluation.friendship_service = services.friendship_service
    test_evaluation.badge_service = services.badge_service
    test_evaluation.evaluation_service = services.evaluation_service
    test_evaluation.awarded_badge_repo = services.awarded_badge_repo

    test_recommendations.friendship_service = services.friendship_service
    test_recommendations.recommendations_service = (
        services.recommendations_service
    )

    test_recommendations_integration.friendship_service = (
        services.friendship_service
    )
    test_recommendations_integration.recommendations_service = (
        services.recommendations_service
    )

    # Hand the composed graph to the activity-wiring fixture below.
    yield services


@pytest.fixture(autouse=True)
def reset_activity_listeners(compose_services):
    """Give every test a clean ``activity`` listener registry.

    The registry is a process-wide, module-level set, so without a reset the
    listeners one test subscribes would leak into the next. After clearing, the
    freshly composed ``EvaluationService`` re-registers its listener -- so
    automatic badge evaluation stays wired for the integration tests without
    leaking any test-local spies. Cleared again on teardown so nothing survives
    into an unrelated test.
    """

    import activity.base

    # ``__listeners`` is module-private with no public reset hook; reaching in
    # here (outside any class, so unmangled) is the reset seam for the tests.
    activity.base.__listeners.clear()
    compose_services.evaluation_service.register()
    yield
    activity.base.__listeners.clear()
