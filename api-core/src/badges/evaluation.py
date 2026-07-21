"""Wires the badges slice into the ``activity`` publish/subscribe registry.

Builds the ``AwardContext`` that ``BadgeService.evaluate_badges`` reads and
subscribes a listener that re-evaluates a user's badges whenever another slice
(e.g. ``friends``) publishes a change for that user, without ``badges`` needing
to import those slices directly.
"""

from __future__ import annotations

import activity
import attendance
import badges
import bridge
import datetime
import friends
import logging
import uuid

_logger = logging.getLogger(__name__)


class EvaluationService:
    """Re-evaluates a user's badges in response to ``activity`` publications.

    Bridges the fact-producing slices (``attendance``, ``friends``) and the
    ``badges`` slice: it assembles an ``AwardContext`` from the injected services
    and hands it to ``BadgeService.evaluate_badges``, awarding any newly earned
    badges and notifying event-service of each one. ``register`` subscribes its
    listener to the ``activity`` registry so this happens automatically whenever
    another slice publishes.
    """

    def __init__(
        self,
        attendance_service: attendance.AttendanceService,
        friendship_service: friends.FriendshipService,
        badge_service: badges.BadgeService
    ):
        self.__attendance_service = attendance_service
        self.__friendship_service = friendship_service
        self.__badge_service = badge_service


    def __retrieve_event_info(
        self,
        event_ids: list[uuid.UUID]
    ) -> list[badges.AttendedEvent]:
        """Helper method which retrieves event info from event-service and
        parses it into a list of ``AttendedEvent`` records.

        Args:
            event_ids: A list of the IDs of the events to lookup

        Returns:
            The ``AttendedEvent`` records of the given Events

        Raises:
            SubprocessError: if an error occurs with the subprocess, including
            if a given event does not exist.
        """
        event_data = bridge.get_batch_event_info(event_ids)

        attended_events: list[badges.AttendedEvent] = []

        for event_id in event_data:
            event = event_data[event_id]
            host_id = uuid.UUID(event["host"])
            category = event["category"]
            start = datetime.datetime.fromisoformat(event["start"])

            attended_event = badges.AttendedEvent(event_id,
                                                 host_id,
                                                 frozenset(category),
                                                 start)
            
            attended_events.append(attended_event)

        return attended_events


    def build_award_context(self, user_id: uuid.UUID) -> badges.AwardContext:
        """Builds the award context for a user from the state of other slices.

        Gathers the raw activity facts that ``evaluate_badges`` needs from their
        respective domain slices. ``messages_sent`` is currently stubbed as an
        empty tuple, pending the messaging feature.

        Args:
            user_id: The ID of the user to build the context for.

        Returns:
            The assembled ``AwardContext`` snapshot for the user.
        """
        # Retrieve event data and construct AttendedEvent records
        attendance_list = self.__attendance_service.get_attendances(user_id)
        events_list = [attendance.event_id for attendance in attendance_list]
        attended_events = tuple(self.__retrieve_event_info(events_list))

        # Retrieve all other relevant data
        friend_count = len(self.__friendship_service.get_friends(user_id))
        messages_sent = () # Empty stub

        context = badges.AwardContext(
            user_id,
            attended_events,
            friend_count,
            messages_sent
        )

        return context


    def evaluation_listener(self, user_id: uuid.UUID) -> None:
        """Re-evaluates a user's badges in response to an activity publication.

        Builds a fresh ``AwardContext`` for the user and passes it to
        ``BadgeService.evaluate_badges``, awarding any badges the user has newly
        become eligible for, then notifies event-service of each newly awarded
        badge.

        Args:
            user_id: The ID of the user whose activity changed.
        """
        awarded_badges = self.__badge_service.evaluate_badges(
            self.build_award_context(user_id)
        )
        for badge in awarded_badges:
            self._notify_badge_awarded(user_id, badge)


    def _notify_badge_awarded(
        self,
        user_id: uuid.UUID,
        badge: badges.Badge
    ) -> None:
        """Best-effort notification to event-service that a badge was
        auto-awarded.

        The badge is already persisted in ``api-core`` by the time this runs, and
        this listener executes synchronously inside ``activity.publish`` -- so a
        failure here would otherwise propagate back to the original publisher
        (e.g. ``record_attendance``) and roll its operation back. A dropped
        cross-service notification must not undo a real award, so bridge failures
        are logged and swallowed rather than raised.

        Args:
            user_id: The ID of the user who was awarded the badge.
            badge: The badge that was awarded.
        """
        try:
            bridge.notify_badge_awarded(user_id, badge.name)
        except bridge.SubprocessError:
            _logger.warning(
                "Failed to notify event-service that user %s was awarded badge"
                " %s (%s)",
                user_id,
                badge.name,
                badge.id,
            )


    def register(self) -> None:
        """Subscribes ``evaluation_listener`` to the ``activity`` registry.

        Wired at the composition root so that any slice publishing an activity
        change automatically triggers badge evaluation, without those slices
        depending on ``badges`` directly.
        """
        activity.subscribe(self.evaluation_listener)
