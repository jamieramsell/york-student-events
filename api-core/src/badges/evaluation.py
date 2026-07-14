"""Wires the badges slice into the ``activity`` publish/subscribe registry.

Builds the ``AwardContext`` that ``evaluate_badges`` reads and subscribes a
listener that re-evaluates a user's badges whenever another slice (e.g.
``friends``) publishes a change for that user, without ``badges`` needing to
import those slices directly.
"""

import activity
import badges
import friends
import uuid


def build_award_context(user_id: uuid.UUID) -> badges.AwardContext:
    """Builds the award context for a user from the state of other slices.

    Gathers the raw activity facts that ``evaluate_badges`` needs from their
    respective domain slices. ``attended_events`` and ``messages_sent`` are
    currently stubbed as empty tuples, pending the ``attendance`` and
    messaging features.

    Args:
        user_id: The ID of the user to build the context for.

    Returns:
        The assembled ``AwardContext`` snapshot for the user.
    """
    attended_events = () # Empty stub
    friend_count = len(friends.get_friends(user_id))
    messages_sent = () # Empty stub

    context = badges.AwardContext(
        user_id,
        attended_events,
        friend_count,
        messages_sent
    )

    return context


def evaluation_listener(user_id: uuid.UUID) -> None:
    """Re-evaluates a user's badges in response to an activity publication.

    Builds a fresh ``AwardContext`` for the user and passes it to
    ``badges.evaluate_badges``, awarding any badges the user has newly become
    eligible for.

    Args:
        user_id: The ID of the user whose activity changed.
    """
    badges.evaluate_badges(build_award_context(user_id))


def register() -> None:
    """Subscribes ``evaluation_listener`` to the ``activity`` registry.

    Called automatically on import of the ``badges`` package so that any
    slice publishing an activity change automatically triggers badge
    evaluation, without those slices depending on ``badges`` directly.
    """
    activity.subscribe(evaluation_listener)


