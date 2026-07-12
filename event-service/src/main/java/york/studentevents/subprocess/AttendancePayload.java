package york.studentevents.subprocess;

import java.util.UUID;

/**
 * Payload carrying the ID of a user and an event, used for requests that record a user's attendance
 * to an event.
 *
 * @param userId the user's ID
 * @param eventId the ID of the event
 */
record AttendancePayload(UUID userId, UUID eventId) implements IPayload {}