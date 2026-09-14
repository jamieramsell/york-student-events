"""Core domain model for the friends slice.

Defines the ``Friendship`` entity, its ``FriendshipStatus`` lifecycle states,
and the ``_generate_id`` helper that derives a Friendship's repository key from
the two user IDs it relates. This is the lowest layer of the friends package: it
depends only on the generic ``repositories`` abstraction and is consumed by both
the repository and service layers.
"""
import dataclasses
import datetime
import enum
import uuid

import repositories

# Type alias for FriendshipId, a key formed by a frozenset of uuid.UUIDs
type FriendshipId = frozenset[uuid.UUID]

def _generate_id(id1: uuid.UUID, id2: uuid.UUID) -> FriendshipId:
    """Utility function to generate the key of a Friendship between two users.
    
    Args:
        id1: the UUID of one friend.
        id2: the UUID of the other friend.

    Returns:
        A frozenset of the two friends' IDs

    Raises:
        ValueError: if the two IDs are equal.
    
    See Also:
        Friendship
    """

    if id1 == id2:
        raise ValueError("id1 and id2 cannot be equal.")
    
    id_list = [id1, id2]
    return frozenset(id_list)

class FriendshipStatus(enum.Enum):
    """
    Enum class defining the two possible states of a friendship.
     
    A friendship can either be awaiting a review by the target friend (the user
    pointed to by friend_id), or it can have been accepted.
    """
    PENDING = "pending"
    ACCEPTED = "accepted"

@dataclasses.dataclass(frozen = True)
class Friendship(repositories.IEntity[FriendshipId]):
    """
    Defines the core structure for friendship service relationship in the 
    api-core service.

    Note that user_id represents the ID of the user who originally sent the
    request; friend_id represents the user who receives and must approve/decline
    said request.

    Args:
        user_id: The ID of the user sending the request.
        friend_id: The ID of the user receiving the request.
        created_at: The datetime of when the friend request was sent.
        friendship_status: Whether or not the friend request has been accepted
            yet.
    """
    user_id: uuid.UUID
    friend_id: uuid.UUID
    created_at: datetime.datetime
    friendship_status: FriendshipStatus

    def get_id(self) -> FriendshipId:
        return _generate_id(self.user_id, self.friend_id)
