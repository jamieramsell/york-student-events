# api-core/tests/test_attendance.py
"""Integration tests for the attendance slice's service layer.

Exercises ``attendance.attendance_service``, so the tests pin the behaviour
callers actually get, rather than that of a hand-injected fixture.

Coverage mirrors the service contract: recording (field + ``recorded_at``
timestamp storage, duplicate rejection without overwrite, re-record after
withdraw, two events for one user both persisting), withdrawal (targeted
removal, absent-record error), ``has_attended`` (true / false / after-withdraw /
event discrimination), ``get_attendances`` (per-user scoping, returning
``Attendance`` records) and ``get_event_attendees`` (per-event scoping,
returning attendee ids).

Run from the repo root:  ``python -m pytest api-core/tests/``
"""

import datetime
import uuid

import pytest
from attendance import AttendanceService, InMemoryAttendanceRepository
from attendance.base import Attendance

# Module-level defaults so the file is usable on its own; ``compose_services`` in
# conftest.py swaps in a fresh, isolated service (and its repository) before
# every test.
attendance_repo = InMemoryAttendanceRepository()
attendance_service = AttendanceService(attendance_repo)


def _repo():
    """Returns the repository backing the service under test."""

    return attendance_repo


def _record(attendee_id=None, event_id=None):
    """Records an attendance with fresh, overridable ids.

    Returns the ``(attendee_id, event_id)`` pair used so callers can go on to
    query or withdraw the record just written.
    """

    attendee_id = attendee_id if attendee_id is not None else uuid.uuid4()
    event_id = event_id if event_id is not None else uuid.uuid4()

    attendance_service.record_attendance(attendee_id, event_id)
    return attendee_id, event_id


# ---------------------------------------------------------------------------
# record_attendance
# ---------------------------------------------------------------------------
class TestRecordAttendance:

    def test_record_creates_a_record_with_the_given_fields(self):
        attendee_id = uuid.uuid4()
        event_id = uuid.uuid4()

        attendance_service.record_attendance(attendee_id, event_id)

        record = _repo().find_by_id((attendee_id, event_id))
        assert record is not None
        assert isinstance(record, Attendance)
        assert record.attendee_id == attendee_id
        assert record.event_id == event_id

    def test_record_stamps_a_datetime_recorded_at(self):
        before = datetime.datetime.now()
        attendee_id, event_id = _record()
        after = datetime.datetime.now()

        record = _repo().find_by_id((attendee_id, event_id))
        assert isinstance(record.recorded_at, datetime.datetime)
        assert before <= record.recorded_at <= after

    def test_duplicate_record_raises_and_does_not_overwrite(self):
        attendee_id, event_id = _record()
        original = _repo().find_by_id((attendee_id, event_id))

        with pytest.raises(ValueError):
            attendance_service.record_attendance(attendee_id, event_id)

        # The stored record must be untouched by the rejected second call.
        assert _repo().find_by_id((attendee_id, event_id)) == original

    def test_re_record_after_withdraw_succeeds(self):
        attendee_id, event_id = _record()

        attendance_service.withdraw_attendance(attendee_id, event_id)
        attendance_service.record_attendance(attendee_id, event_id)

        assert _repo().find_by_id((attendee_id, event_id)) is not None

    def test_two_events_for_one_user_both_persist(self):
        attendee_id = uuid.uuid4()
        event_a = uuid.uuid4()
        event_b = uuid.uuid4()

        _record(attendee_id=attendee_id, event_id=event_a)
        _record(attendee_id=attendee_id, event_id=event_b)

        assert _repo().find_by_id((attendee_id, event_a)) is not None
        assert _repo().find_by_id((attendee_id, event_b)) is not None
        assert len(attendance_service.get_attendances(attendee_id)) == 2


# ---------------------------------------------------------------------------
# withdraw_attendance
# ---------------------------------------------------------------------------
class TestWithdrawAttendance:

    def test_withdraw_removes_only_the_targeted_record(self):
        attendee_id = uuid.uuid4()
        kept_event = uuid.uuid4()
        withdrawn_event = uuid.uuid4()

        _record(attendee_id=attendee_id, event_id=kept_event)
        _record(attendee_id=attendee_id, event_id=withdrawn_event)

        attendance_service.withdraw_attendance(attendee_id, withdrawn_event)

        assert _repo().find_by_id((attendee_id, withdrawn_event)) is None
        assert _repo().find_by_id((attendee_id, kept_event)) is not None

    def test_withdraw_absent_record_raises(self):
        with pytest.raises(ValueError):
            attendance_service.withdraw_attendance(uuid.uuid4(), uuid.uuid4())


# ---------------------------------------------------------------------------
# has_attended
# ---------------------------------------------------------------------------
class TestHasAttended:

    def test_has_attended_true_after_recording(self):
        attendee_id, event_id = _record()

        assert attendance_service.has_attended(attendee_id, event_id) is True

    def test_has_attended_false_when_never_recorded(self):
        assert (
            attendance_service.has_attended(uuid.uuid4(), uuid.uuid4()) is False
        )

    def test_has_attended_false_after_withdraw(self):
        attendee_id, event_id = _record()
        attendance_service.withdraw_attendance(attendee_id, event_id)

        assert attendance_service.has_attended(attendee_id, event_id) is False

    def test_has_attended_distinguishes_events(self):
        attendee_id = uuid.uuid4()
        attended_event = uuid.uuid4()
        other_event = uuid.uuid4()

        _record(attendee_id=attendee_id, event_id=attended_event)

        assert (
            attendance_service.has_attended(attendee_id, attended_event) is True
        )
        assert (
            attendance_service.has_attended(attendee_id, other_event) is False
        )


# ---------------------------------------------------------------------------
# get_attendances
# ---------------------------------------------------------------------------
class TestGetAttendances:

    def test_get_attendances_returns_only_the_target_users_records(self):
        target = uuid.uuid4()
        other = uuid.uuid4()

        target_event_a = uuid.uuid4()
        target_event_b = uuid.uuid4()
        _record(attendee_id=target, event_id=target_event_a)
        _record(attendee_id=target, event_id=target_event_b)
        _record(attendee_id=other, event_id=uuid.uuid4())

        results = attendance_service.get_attendances(target)

        assert {record.event_id for record in results} == {
            target_event_a,
            target_event_b,
        }
        assert all(record.attendee_id == target for record in results)

    def test_get_attendances_empty_for_unknown_user(self):
        _record()  # unrelated record for a different user

        assert attendance_service.get_attendances(uuid.uuid4()) == []


# ---------------------------------------------------------------------------
# get_event_attendees  (returns a list of attendee ids, not records)
# ---------------------------------------------------------------------------
class TestGetEventAttendees:

    def test_get_event_attendees_returns_only_the_target_events_attendees(self):
        target_event = uuid.uuid4()
        other_event = uuid.uuid4()

        attendee_a = uuid.uuid4()
        attendee_b = uuid.uuid4()
        outsider = uuid.uuid4()
        _record(attendee_id=attendee_a, event_id=target_event)
        _record(attendee_id=attendee_b, event_id=target_event)
        _record(attendee_id=outsider, event_id=other_event)

        results = attendance_service.get_event_attendees(target_event)

        assert set(results) == {attendee_a, attendee_b}
        assert outsider not in results

    def test_get_event_attendees_returns_attendee_ids_not_records(self):
        attendee_id, event_id = _record()

        results = attendance_service.get_event_attendees(event_id)

        assert results == [attendee_id]
        assert all(isinstance(item, uuid.UUID) for item in results)

    def test_get_event_attendees_empty_for_unknown_event(self):
        _record()  # unrelated record for a different event

        assert attendance_service.get_event_attendees(uuid.uuid4()) == []

    def test_get_event_attendees_reflects_a_withdrawal(self):
        event_id = uuid.uuid4()
        staying = uuid.uuid4()
        leaving = uuid.uuid4()
        _record(attendee_id=staying, event_id=event_id)
        _record(attendee_id=leaving, event_id=event_id)

        attendance_service.withdraw_attendance(leaving, event_id)

        assert attendance_service.get_event_attendees(event_id) == [staying]

    def test_get_event_attendees_separates_a_users_two_events(self):
        attendee_id = uuid.uuid4()
        event_a = uuid.uuid4()
        event_b = uuid.uuid4()
        _record(attendee_id=attendee_id, event_id=event_a)
        _record(attendee_id=attendee_id, event_id=event_b)

        assert attendance_service.get_event_attendees(event_a) == [attendee_id]
        assert attendance_service.get_event_attendees(event_b) == [attendee_id]