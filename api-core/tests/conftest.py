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