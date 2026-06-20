import dataclasses
import datetime
import enum
import repositories
import uuid

def _generate_id(id1: uuid.UUID, id2: uuid.UUID) -> frozenset:
    """Utility function to generate the key of a Friendship between two users.
    
    Args:
        id1: the UUID of one friend.
        id2: the UUID of the other friend.

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
class Friendship(repositories.IEntity):
    """
    Defines the core structure for friendship service relationship in the 
    api-core service.

    Note that user_id represents the ID of the user who originally sent the
    request; friend_id represents the user who recieves and must approve/decline
    said request.

    All attributes are read-only, except for friendship_status, which can be
    updated from PENDING to ACCEPTED by calling the accept() method.

    Args:
        user_id: The ID of the user sending the request.
        friend_id: The ID of the user recieving the request.
        created_at: The datetime of when the friend request was sent.
        friendship_status: Whether or not the friend request has been accepted
            yet.
    """
    user_id: uuid.UUID
    friend_id: uuid.UUID
    created_at: datetime.datetime
    friendship_status: FriendshipStatus

    def get_id(self) -> frozenset:
        return _generate_id(self.user_id, self.friend_id)
