"""Pytest configuration and shared fixtures for the api-core test suite.

The api-core modules use flat intra-package imports (e.g. ``import base``,
``import repositories``, ``import friendship_repository``) rather than
package-qualified ones, so the source roots must be on ``sys.path`` for the
suite to resolve them. This file adds them so the suite runs from the repo root
with ``python -m pytest api-core/tests/``.

It also resets the friends repository singleton before every test: the service
layer operates on a module-level ``friendship_repository._repository`` instance,
and without a reset that state would leak between tests.
"""

import os
import sys

import pytest

_SRC = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "src"))
_FRIENDS = os.path.join(_SRC, "friends")

for _path in (_SRC, _FRIENDS):
    if _path not in sys.path:
        sys.path.insert(0, _path)


@pytest.fixture(autouse=True)
def reset_repository():
    """Give every test a clean, isolated friends repository.

    The service layer reads ``friendship_repository._repository`` at call time,
    so replacing the singleton here is picked up by all service functions.
    """

    import friendship_repository

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
