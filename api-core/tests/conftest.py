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
