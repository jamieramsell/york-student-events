import dataclasses
import datetime
import enum
import uuid

class FriendshipStatus(enum.Enum):
    """
    Enum class defining the two possible states of a friendship.
     
    A friendship can either be awaiting a review by the target friend (the user
    pointed to by friend_id), or it can have been accepted.
    """
    PENDING = "pending"
    ACCEPTED = "accepted"

@dataclasses.dataclass
class Friendship:
    """
    Defines the core structure for friendship service relationship in the 
    api-core service.

    Note that user_id represents the ID of the user who originally sent the
    request; friend_id represents the user who recieves and must approve/decline
    said request.

    Args:
        user_id: The ID of the user sending the request.
        friend_id: The ID of the user recieving the request.
        created_at: The datetime of when the friend request was sent.
        friendship_status: Whether or not the friend request has been accepted
            yet.
    """
    user_id: int
    friend_id: int
    created_at: datetime.datetime
    friendship_status: FriendshipStatus

friendship_records = {}

def __dictKey(id1: uuid.UUID, id2: uuid.UUID) -> frozenset:
    """
    Utility function to generate the key of a Friendship between two users.
    
    See Also:
        Friendship
    """
    id_list = [id1, id2]
    return frozenset(id_list)

def sendFriendRequest(user_id: uuid.UUID, friend_id: uuid.UUID) -> None:
    """
    Creates a new Friendship record with a PENDING status.

    See Also:
        Friendship
        FriendshipStatus
    """
    request_sent = datetime.datetime.now()
    status = FriendshipStatus.PENDING
    key = __dictKey(user_id, friend_id)

    # Store record in dict
    friendship_records[key] = Friendship(user_id, friend_id, request_sent,
                                            status)

def acceptFriendRequest(user_id: uuid.UUID, friend_id: uuid.UUID) -> None:
    """
    Updates the status of a friendship record to ACCEPTED.

    See Also:
        Friendship
        FriendshipStatus
    """
    key = __dictKey(user_id, friend_id)
    status = FriendshipStatus.ACCEPTED

    # Update record in dict
    friendship_records[key].status = status

def removeFriend(user_id: uuid.UUID, friend_id: uuid.UUID) -> None:
    pass

def getFriends(user_id: uuid.UUID) -> None:
    pass

def isFriend(user_id: uuid.UUID, friend_id: uuid.UUID) -> bool:
    pass