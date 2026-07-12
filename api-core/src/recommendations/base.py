import bridge
import collections
import friends
import uuid

def get_recommended_events(user_id: uuid.UUID) -> list[uuid.UUID]:
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
    
    friends_list = friends.get_friends(user_id)

    # If the user has no friends, there are no events to recommend
    if len(friends_list) == 0: 
        return []
    
    # Add events that the user is already registered for to this set, so that
    # they can be excluded from the list of recommendations
    events_already_registered = set[uuid.UUID](bridge.get_user_events(user_id))
        
    # Make use of a collections.Counter to sort event recommendations by the
    # number of friends attending
    recommended_events = collections.Counter[uuid.UUID]()

    # For each friend, add the events that they have signed up for to the
    # Counter. Ignore any events which the user has already signed up to
    # themselves.
    for friend_id in friends_list:

        # Add the event IDs that the friend is signed up for
        friend_events = set[uuid.UUID](bridge.get_user_events(friend_id))
        
        # Remove events from the set if the user has already signed up to them
        friend_events = set(event for event in friend_events
                            if event not in events_already_registered)

        recommended_events.update(friend_events)
    
    # Iterate through sorted list of tuples, and only return the event IDs, not
    # their number of occurences.
    return [event_id for event_id, _ in recommended_events.most_common()]


def __iterate_over_graph_layers(
    friend_circle: dict[int, list[uuid.UUID]], 
    target: int,
    current_layer: int = 1
) -> list[uuid.UUID]:
    """Recursive helper method used to return a number of recommended friends
    by iterating through each layer of the given friend graph.
    
    Args:
        friend_circle: The graph / friend circle to iterate.
        target: The number of recommendations to aim for.
        current_layer: The layer from which to commence the search.

    Returns:
        Mutual friends, in the form of a list of UUIDs, in decreasing order of
        the depth of the graph from which the recommended friend was found.
        Never ``None``, but may be empty.

    Raises:
        IndexError: if the value of ``current_layer`` is less than one (the
            method does not return existing friends), or it exceeds the number
            of layers in the given graph.
    """
    layers = friend_circle.keys()
    num_layers = len(layers)

    if current_layer < 1 or current_layer > num_layers - 1:
        raise ValueError("Value of current_layer out of bounds."
                         "\nExpected current_layer to fall within boundaries of"
                         " 1 <= x < the number of layers in the graph."
                         f"\nValue recieved: {current_layer}")
    
    # If the current_layer is the deepest layer of the graph, then return, as
    # there are no more users to recommend.
    if current_layer == num_layers - 1:
        return friend_circle[current_layer].copy()
    
    # If the number of users in the current layer reaches the target, return
    if len(friend_circle[current_layer]) >= target: 
        return friend_circle[current_layer].copy()
    
    # If neither stopping condition has been met, then call the function again.
    current_recommendations = friend_circle[current_layer].copy()
    new_target = target - len(friend_circle[current_layer])
    more_recommendations = __iterate_over_graph_layers(friend_circle,
                                                       new_target,
                                                       current_layer + 1)
    
    current_recommendations.extend(more_recommendations)
    return current_recommendations


def find_new_friends(user_id: uuid.UUID) -> list[uuid.UUID]:
    """Recommend new friends for a user, based on their mutual friends.

    Builds a list of users that share a friend with the given user, ordered by
    how many friends they have in common. Users who already have a friendship
    (either pending or accepted) with the given user are excluded.

    If no users who share a common friend with the given user exist, then the
    algorithm will attempt to delve deeper into the given user's friend circle
    in order to find some who may be slightly further away. 

    Args:
        user_id: The ID of the user to generate recommendations for.

    Returns:
        A list of user IDs, ordered by descending number of mutual friends.
        Returns an empty list if the user has no friends.

    See Also:
        friends.friendship_service: the service used to generate friendship
            circles.
    """
    friend_circle = friends.get_friend_circle(user_id, 2)
    TARGET_RECOMMENDATIONS = 10

    return __iterate_over_graph_layers(friend_circle, TARGET_RECOMMENDATIONS)

    