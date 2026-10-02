"""Friend graph slice for the api-core service.

This package models the social graph between students: friend requests, their
acceptance lifecycle, and queries over established friendships. It builds on the
generic ``repositories`` abstraction, storing ``Friendship`` entities in an
in-memory repository today and remaining open to a database-backed backend
later.

The public surface is the ``Friendship`` entity and its ``FriendshipStatus``,
the friendship repositories, and the service-level operations callers use to
drive the friendship graph (send/accept/remove requests and query friendships).
"""

from friends.base import Friendship, FriendshipStatus
from friends.friendship_repository import (
    InMemoryFriendshipRepository,
    SQLAlchemyFriendshipRepository,
)
from friends.friendship_service import FriendshipRepository, FriendshipService

__all__ = [
    "Friendship",
    "FriendshipRepository",
    "FriendshipService",
    "FriendshipStatus",
    "InMemoryFriendshipRepository",
    "SQLAlchemyFriendshipRepository"
]
