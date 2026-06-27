"""Friend graph slice for the api-core service.

This package models the social graph between students: friend requests, their
acceptance lifecycle, and queries over established friendships. It builds on the
generic ``repositories`` abstraction, storing ``Friendship`` entities in an
in-memory repository today and remaining open to a database-backed backend
later.

The public surface is the ``Friendship`` entity and its ``FriendshipStatus``,
the in-memory repository, and the service-level operations callers use to drive
the friend graph (send/accept/remove requests and query friendships).
"""

from friends.base import Friendship, FriendshipStatus
from friends.friendship_repository import InMemoryFriendshipRepository
from friends.friendship_service import (
    accept_friend_request,
    get_friends,
    is_friend,
    remove_friend,
    send_friend_request,
)

__all__ = [
    "Friendship",
    "FriendshipStatus",
    "InMemoryFriendshipRepository",
    "accept_friend_request",
    "get_friends",
    "is_friend",
    "remove_friend",
    "send_friend_request",
]
