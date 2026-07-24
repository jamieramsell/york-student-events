"""Service-level operations for the attendance slice.

Exposes the attendance use cases callers depend on, such as recording a
student's attendance at an event, removing an attendance record, and querying
whether a student attended a given event or which students attended it.
Orchestrates the ``Attendance`` domain model from ``base`` with the in-memory
repository, keeping persistence details out of callers.
"""
import datetime
import uuid

import activity
import repositories

import attendance.base as base

type AttendanceRepository = repositories.IRepository[base.AttendanceId,
                                                     base.Attendance]

class AttendanceService:
    """Service-level operations for the attendance slice.

    Exposes the attendance use cases callers depend on, such as recording a
    student's attendance at an event, removing an attendance record, and 
    querying whether a student attended a given event or which students attended
    it. Orchestrates the ``Attendance`` domain model from ``base`` with the
    in-memory repository, keeping persistence details out of callers.
    """

    def __init__(self, attendance_repository: AttendanceRepository):
        self.__attendance_repository = attendance_repository

    def record_attendance(
        self,
        attendee_id: uuid.UUID,
        event_id: uuid.UUID
    ) -> None:
        """Creates a new Attendance record, timestamped to the current time.

        Args:
            attendee_id: The ID of the Student attending the Event.
            event_id: The ID of the Event.

        Raises:
            ValueError: if the user's attendance to that event has already been
                recorded.

        See Also:
            Attendance
        """
        record_id = base._generate_id(attendee_id, event_id)
        existing_record = self.__attendance_repository.find_by_id(record_id)
        if existing_record is not None:
            raise ValueError("The user's attendance has already been recorded.")

        attendance_record = base.Attendance(
            attendee_id, event_id, datetime.datetime.now()
        )
        self.__attendance_repository.save(attendance_record)

        activity.publish(attendee_id)


    def withdraw_attendance(self, attendee_id: uuid.UUID, event_id: uuid.UUID) -> None:
        """Removes the Attendance record for a student/event pair.

        Args:
            attendee_id: The ID of the Student attending the Event.
            event_id: The ID of the Event.

        Raises:
            ValueError: if no Attendance record exists for the pair.

        See Also:
            Attendance
        """
        try:
            self.__attendance_repository.delete(
                base._generate_id(attendee_id, event_id)
            )
        except KeyError:
            raise ValueError("No attendance record for the given event exists"
                            + " for the given user.")


    def has_attended(self, attendee_id: uuid.UUID, event_id: uuid.UUID) -> bool:
        """Checks whether a student has an Attendance record for an event.

        Args:
            attendee_id: The ID of the Student attending the Event.
            event_id: The ID of the Event.

        Returns:
            Whether an Attendance record exists for the pair.
        """
        record_id = base._generate_id(attendee_id, event_id)
        return self.__attendance_repository.find_by_id(record_id) is not None


    def get_attendances(self, attendee_id: uuid.UUID) -> list[base.Attendance]:
        """Retrieves all Attendance records for a given student.

        Args:
            attendee_id: The ID of the Student whose attendances are retrieved.

        Returns:
            A list of the student's Attendance records; never None, may be
            empty.
        """
        all_attendance_records = self.__attendance_repository.find_all()
        user_attendances = [record for record in all_attendance_records
                            if record.attendee_id == attendee_id]
        return user_attendances


    def get_event_attendees(self, event_id: uuid.UUID) -> list[uuid.UUID]:
        """Retrieves the IDs of all Students who attended a given Event.

        Args:
            event_id: The ID of the Event whose attendances are to be retrieved.

        Returns:
            A list of the user IDs of the Students who attended the Event; never
            None, may be empty.
        """
        all_attendance_records = self.__attendance_repository.find_all()
        students_who_attended = [record.attendee_id
                                for record in all_attendance_records
                                if record.event_id == event_id]
        return students_who_attended