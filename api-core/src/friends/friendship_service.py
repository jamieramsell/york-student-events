"""Service-level operations for the friends slice.

Exposes the friend-graph use cases callers depend on -- sending, accepting, and
removing friend requests, and querying a user's friends or whether two users are
friends. Orchestrates the ``Friendship`` domain model from ``base`` with the
in-memory repository, keeping persistence details out of callers.
"""

import friends.base as base
import datetime
import friends.friendship_repository as friendship_repository
import friends.connections as connections
import uuid

def send_friend_request(user_id: uuid.UUID, friend_id: uuid.UUID) -> None:
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

def accept_friend_request(id1: uuid.UUID, id2: uuid.UUID) -> None:
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
    friendship = friendship_repository._repository.find_by_id(friendship_id)
    
    if friendship is None:
        raise ValueError("No friendship record exists between the two users.")
    elif friendship.friendship_status == base.FriendshipStatus.ACCEPTED:
        raise ValueError("An accepted friend request already exists between"
                            + " the two users.")
    
    # Create a new Friendship instance as they are immutable.
    status = base.FriendshipStatus.ACCEPTED
    friendship = base.Friendship(friendship.user_id,
                                friendship.friend_id,
                                friendship.created_at,
                                status)
    friendship_repository._repository.save(friendship)

def remove_friend(id1: uuid.UUID, id2: uuid.UUID) -> None:
    """Removes the Frienship record between two users from the repository.

    Args:
        id1: The ID of one friend.
        id2: The ID of the other friend.
    
    Raises:
        ValueError: if no friendship exists between the two users

    See Also:
        Friendship
    """

    friendship_id = base._generate_id(id1, id2)

    try:
        friendship_repository._repository.delete(friendship_id)
    except KeyError:
        raise ValueError("No friendship exists between the two users.")

def get_friends(user_id: uuid.UUID) -> list[uuid.UUID]:
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
    friend_list: list[uuid.UUID] = []

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

def get_pending_requests(user_id: uuid.UUID) -> list[uuid.UUID]:
    """Retrieves the IDs of users with a pending friendship with the user.

    This is the counterpart to ``get_friends``: it yields only friendships that
    are still awaiting acceptance, and excludes those that have been accepted.
    The direction of the request is not distinguished -- both requests sent by
    the user and requests received by the user are included.

    Args:
        user_id: The ID of the target user.

    Returns:
        A list of the IDs of users with a pending request to or from the target
        user; will never be None, but may be empty.
    """

    # Retrieve all friendships from the repository and initialise the user's
    # pending list.
    all_friendships = friendship_repository._repository.find_all()
    pending_list: list[uuid.UUID] = []

    for friendship in all_friendships:

        # If the current friendship is pending and involves the target user,
        # then add the ID of the other party to the list.
        if (user_id in friendship.get_id()
            and friendship.friendship_status == base.FriendshipStatus.PENDING):

            if friendship.user_id == user_id:
                pending_list.append(friendship.friend_id)
            else:
                pending_list.append(friendship.user_id)

    return pending_list

def is_friend(id1: uuid.UUID, id2: uuid.UUID) -> bool:
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


def get_friend_circle(
    user_id: uuid.UUID,
    grouping: bool = True,
    max_layers: int | None = None
    ) -> dict[int, set[uuid.UUID]] | set[uuid.UUID]:
    """Algorithm used to discover and generate the layers of the interactive
    maps of friend networks.

    If grouping is enabled (which is the default option), results are generated
    as a dictionary, with integer keys which represent the number of friends
    between two users, and set[UUID] values, which store the IDs of the users
    at that given level.

    The users in layer 0 are those who are directly friends with the target
    user, users in layer 1 share a common friend with the user, users in layer 2
    have a connection with the given user through two other friends, and so on.

    If grouping is disabled, the program uses a more efficient depth-first
    searching algorithm (as opposed to breadth-first), and returns the connected
    users' IDs in a plain set. In this mode ``max_layers`` is ignored: the whole
    reachable circle is returned, as depth-bounding a depth-first traversal
    cannot reliably yield every user within a given distance.

    In either mode the target user is never included in the result.

    Args:
        user_id: The target user's ID.
        grouping: whether or not to group the circle into layers.
        max_layers: The maximum depth to generate. A value of 1 means that
            layers 0 and 1 will be generated. Ignored when ``grouping`` is
            ``False``.
        
    Returns:
        If grouping is enabled, returns a ``dict[int, set[uuid.UUID]]``, where
        integer keys represent the layers (the number of friends that exist
        on the path between two users), and set values, which store
        the IDs of the users at that given level.

        If grouping is disabled, returns a simple set of connected users' IDs.

        Return results are never ``None``, but may be empty.

    Raises:
        ValueError: if the given user could not be found.
        ValueError: if ``max_layers < 0``.
    """

    if max_layers is not None and max_layers < 0:
        raise ValueError("max_layers must not be negative.")

    # api-core has no user registry, so the friend graph is the only source of
    # truth for whether a user exists: a user is "known" if they participate in
    # at least one friendship record (pending or accepted).
    all_friendships = friendship_repository._repository.find_all()
    user_known = any(user_id in friendship.get_id()
                     for friendship in all_friendships)

    if not user_known:
        raise ValueError("The given user could not be found: either they don't"
                         + " exist, or they have no friends.")

    if grouping:
        return connections.bfs_friend_layers(user_id, max_layers)

    # grouping disabled: a flat depth-first sweep of the user's whole connected
    # component -- the efficient path for large circles where only reachability,
    # not distance, matters. dfs_recursion seeds its visited set with the target
    # user, so remove them to match the grouped form (which never includes the
    # target). max_layers is intentionally not threaded through here.
    reachable = connections.dfs_recursion(user_id, set())
    reachable.discard(user_id)
    return reachable