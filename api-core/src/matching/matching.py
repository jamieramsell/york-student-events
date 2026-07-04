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
