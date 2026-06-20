import dataclasses
import datetime
import enum
import friendship_repository
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
        raise(ValueError, "id1 and id2 cannot be equal.")
    
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
    
    def _accept(self) -> None:
        """Updates the status of the friend request to ACCEPTED."""

        self.friendship_status = FriendshipStatus.ACCEPTED

repository = friendship_repository.InMemoryFriendshipRepository()

def sendFriendRequest(user_id: uuid.UUID, friend_id: uuid.UUID) -> None:
    """Creates a new Friendship record with a PENDING status.

    Args:
        user_id: The user sending the request.
        friend_id: The user recieving the request.

    See Also:
        Friendship
        FriendshipStatus
    """

    time = datetime.datetime.now()
    status = FriendshipStatus.PENDING
    friendship = Friendship(user_id, friend_id, time, status)

    repository.save(friendship)

def acceptFriendRequest(id1: uuid.UUID, id2: uuid.UUID) -> None:
    """Updates the status of a friendship record to ACCEPTED.

    Args:
        id1: The ID of one friend.
        id2: The ID of the other friend.

    See Also:
        Friendship
        FriendshipStatus
    """

    friendship_id = _generate_id(id1, id2)

    friendship = repository.find_by_id(friendship_id)
    friendship._accept()
    repository.save(friendship_id)

def removeFriend(id1: uuid.UUID, id2: uuid.UUID) -> None:
    """Removes the Frienship record between two users from the repository.

    Args:
        id1: The ID of one friend.
        id2: The ID of the other friend.
        
    See Also:
        Friendship
    """

    friendship_id = _generate_id(id1, id2)
    repository.delete(friendship_id)

def getFriends(user_id: uuid.UUID) -> list[uuid.UUID]:
    """Retrieves a list of user IDs of the user's friends.

    Note that this method will not yield the user IDs of pending friend
    requests, only those that have been accepted.

    Args:
        user_id: The ID of the target user.

    Returns:
        A list of the IDs of the user's friends; will never be None, but may be
        empty.
    """
    
    # Retrieve all friendships from the repository and initialise the user's
    # friend list.
    all_friendships = repository.find_all()
    friend_list = []

    for friendship in all_friendships:

        # If the current friendship involves the target user, then add the ID of
        # the friend to the friend list
        if friendship.get_id().__contains__(user_id):

            friendship_sender_id = friendship.user_id
            friendship_receiver_id = friendship.friend_id

            if friendship_sender_id == user_id:
                friend_list.append(friendship_receiver_id)
            else:
                friend_list.append(friendship_sender_id)
    
    return friend_list

def isFriend(id1: uuid.UUID, id2: uuid.UUID) -> bool:
    """Checks whether two users are friends.

    Note that the method will return True only if a friend request between the
    two users has been accepted; pending friend requests will still return
    False.

    Args:
        id1: The ID of one friend.
        id2: The ID of the other friend.
    
    Returns:
        Whether the two users are friends.
    """

    friendship_id = _generate_id(id1, id2)
    friendship = repository.find_by_id(friendship_id)

    # Predicate resolves to true if the friendship exists within the repository;
    # false if not.
    return (friendship is not None) 