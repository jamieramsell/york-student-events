import bridge
import collections
import friends
import uuid


def _iterate_over_graph_layers(
    friend_circle: dict[int, set[uuid.UUID]],
    target: int,
    current_layer: int = 1
) -> set[uuid.UUID]:
    """Recursive helper used to return a number of recommended friends by
    iterating through each layer of the given friend graph.

    Gathers users from ``current_layer`` outwards, descending a layer at a time,
    until at least ``target`` users have been collected or the graph runs out of
    layers.

    Args:
        friend_circle: The graph / friend circle to iterate.
        target: The number of recommendations to aim for.
        current_layer: The layer from which to commence the search.

    Returns:
        Mutual friends, in the form of a set of UUIDs, gathered from
        ``current_layer`` outwards. Never ``None``, but may be empty (e.g. the
        graph does not reach ``current_layer``).

    Raises:
        ValueError: if the value of ``current_layer`` is less than one (the
            method does not return the user's existing friends, held at layer 0).
    """
    if current_layer < 1:
        raise ValueError("Value of current_layer out of bounds."
                         "\nExpected current_layer to be at least 1; layer 0"
                         " holds the user's existing friends."
                         f"\nValue received: {current_layer}")

    # The graph does not reach this depth, so there are no further users to
    # recommend from here.
    if current_layer not in friend_circle:
        return set()

    # Defensive copy so the caller's friend circle is never mutated.
    current_recommendations = set(friend_circle[current_layer])

    # Stop once this layer alone meets the target, or when it is the deepest
    # layer the graph reaches (there is no next layer to descend into).
    if (len(current_recommendations) >= target
            or (current_layer + 1) not in friend_circle):
        return current_recommendations

    # Otherwise, top up the recommendations from the next layer out.
    new_target = target - len(current_recommendations)
    more_recommendations = _iterate_over_graph_layers(friend_circle,
                                                      new_target,
                                                      current_layer + 1)

    return current_recommendations.union(more_recommendations)


class RecommendationsService:
    """Service-level recommendation algorithms for the events app.

    Generates event and friend recommendations for a user by mining the friend
    graph (through the injected ``FriendshipService``) and, for events, the
    events each user is registered for (fetched from event-service over the
    subprocess bridge).
    """

    def __init__(self, friendship_service: friends.FriendshipService):
        self.__friendship_service = friendship_service

    def get_recommended_events(self, user_id: uuid.UUID) -> list[uuid.UUID]:
        """Recommend events for a user based on their friends' attendance.

        Builds a list of events that the user's friends have signed up for,
        ranked by how many friends are attending each one. Events the user has
        already registered for are excluded.

        Args:
            user_id: The ID of the user to generate recommendations for.

        Returns:
            A list of event IDs ordered by descending number of attending
            friends. Returns an empty list if the user has no friends.
        """

        friends_list = self.__friendship_service.get_friends(user_id)

        # If the user has no friends, there are no events to recommend
        if len(friends_list) == 0:
            return []

        # Add events that the user is already registered for to this set, so
        # that they can be excluded from the list of recommendations
        events_already_registered = set[uuid.UUID](
            bridge.get_user_events(user_id)
        )

        # Make use of a collections.Counter to sort event recommendations by the
        # number of friends attending
        recommended_events = collections.Counter[uuid.UUID]()

        # For each friend, add the events that they have signed up for to the
        # Counter. Ignore any events which the user has already signed up to
        # themselves.
        for friend_id in friends_list:

            # Add the event IDs that the friend is signed up for
            friend_events = set[uuid.UUID](bridge.get_user_events(friend_id))

            # Remove events from the set if the user has already signed up to
            # them
            friend_events = set(event for event in friend_events
                                if event not in events_already_registered)

            recommended_events.update(friend_events)

        # Iterate through sorted list of tuples, and only return the event IDs,
        # not their number of occurrences.
        return [event_id for event_id, _ in recommended_events.most_common()]


    def find_new_friends(self, user_id: uuid.UUID) -> set[uuid.UUID]:
        """Recommend new friends for a user, based on their mutual friends.

        Builds a set of users that share a friend with the given user. Users who
        already have a friendship (either pending or accepted) with the given
        user are excluded.

        If no users who share a common friend with the given user exist, then
        the algorithm will attempt to delve deeper into the given user's friend
        circle in order to find some who may be slightly further away.

        Note that this does not verify that the user exists: a user with no
        friends is indistinguishable from one absent from the graph entirely,
        and both simply yield an empty set.

        Args:
            user_id: The ID of the user to generate recommendations for.

        Returns:
            A set of recommended user IDs. Returns an empty set if the user has
            no friends, or if none can be found within reach of their friend
            circle.

        See Also:
            friends.FriendshipService: the service used to generate friendship
                circles.
        """
        # A user with no accepted friends has no friend circle to mine, so there
        # is nothing to recommend. Checking here also sidesteps
        # get_friend_circle's "user not found" error for users who are absent
        # from the graph entirely.
        if len(self.__friendship_service.get_friends(user_id)) == 0:
            return set()

        friend_circle = self.__friendship_service.get_friend_circle(
            user_id, max_layers=2
        )
        TARGET_RECOMMENDATIONS = 10

        recommended = _iterate_over_graph_layers(friend_circle,
                                                 TARGET_RECOMMENDATIONS)

        # Users with an existing (pending) request to or from the user are not
        # new friends to recommend. Accepted friends already sit at layer 0,
        # which the helper skips, so only pending relationships need removing
        # here.
        pending = set(self.__friendship_service.get_pending_requests(user_id))

        return recommended - pending
