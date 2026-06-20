"""Service-level operations for the friends slice.

Exposes the friend-graph use cases callers depend on -- sending, accepting, and
removing friend requests, and querying a user's friends or whether two users are
friends. Orchestrates the ``Friendship`` domain model from ``base`` with the
in-memory repository, keeping persistence details out of callers.
"""

import base
import datetime
import friendship_repository
import uuid

def sendFriendRequest(user_id: uuid.UUID, friend_id: uuid.UUID) -> None:
    """Creates a new Friendship record with a PENDING status.

    Args:
        user_id: The user sending the request.
        friend_id: The user recieving the request.

    Throws:
        ValueError: if a friend request or Friendship already exists between
            the two users.

    See Also:
        Friendship
        FriendshipStatus
    """

    friendship_id = base._generate_id(user_id, friend_id)
    friendship = friendship_repository._repository.find_by_id(friendship_id)

    if friendship is not None:
        raise ValueError("A friend request already exists between the two"
                         + " users, and has either been accepted, or is still"
                         + " pending.")

    time = datetime.datetime.now()
    status = base.FriendshipStatus.PENDING
    friendship = base.Friendship(user_id, friend_id, time, status)

    friendship_repository._repository.save(friendship)

def acceptFriendRequest(id1: uuid.UUID, id2: uuid.UUID) -> None:
    """Updates the status of a friendship record to ACCEPTED.

    Args:
        id1: The ID of one friend.
        id2: The ID of the other friend.

    Raises:
        ValueError: if an accepted friend request already exists between the two
            users
        ValueError: no Friendship record exists between the two users.

    See Also:
        Friendship
        FriendshipStatus
    """

    friendship_id = base._generate_id(id1, id2)

    try:
        friendship = friendship_repository._repository.find_by_id(friendship_id)
        if friendship.friendship_status == base.FriendshipStatus.ACCEPTED:
            raise ValueError("An accepted friend request already exists between"
                             + " the two users.")
        # Create a new Friendship instance as they are immutable.
        status = base.FriendshipStatus.ACCEPTED
        friendship = base.Friendship(friendship.user_id,
                                    friendship.friend_id,
                                    friendship.created_at,
                                    status)
        friendship_repository._repository.save(friendship)

    except KeyError:
        raise ValueError("No friendship record exists between the two users.")

def removeFriend(id1: uuid.UUID, id2: uuid.UUID) -> None:
    """Removes the Frienship record between two users from the repository.

    Args:
        id1: The ID of one friend.
        id2: The ID of the other friend.
        
    See Also:
        Friendship
    """

    friendship_id = base._generate_id(id1, id2)
    friendship_repository._repository.delete(friendship_id)

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
    all_friendships = friendship_repository._repository.find_all()
    friend_list = []

    for friendship in all_friendships:

        # If the current friendship is not pending, and involves the target
        # user, then add the ID of the friend to the list
        if (friendship.get_id().__contains__(user_id)
            and friendship.friendship_status == base.FriendshipStatus.ACCEPTED):

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

    friendship_id = base._generate_id(id1, id2)
    friendship = friendship_repository._repository.find_by_id(friendship_id)

    # Predicate resolves to true if the friendship exists within the repository
    # and is accepted; returns false otherwise.
    return ((friendship is not None)
        and (friendship.friendship_status == base.FriendshipStatus.ACCEPTED))