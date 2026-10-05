"""Seed loader for the api-core service.

Reads the shared ``data/seed.json`` file and populates the api-core repositories
with the Python-side seed data: attendance records, badge definitions, and
friendships. The Java-side arrays (venues, users, events, cohorts, subscriptions)
are ignored here and loaded by event-service's ``InMemorySeededData`` runner.

Intended for use in the in-memory profile only; ``load_seed`` must only be
invoked after having composed the service graph via ``bootstrap.bootstrap()``.
"""
import datetime
import json
import uuid

import attendance
import badges
import badges.predicates
import bootstrap
import friends


def load_seed(services: bootstrap.Services,
              seed_path: str = "../data/seed.json") -> None:
    """Deserialises the Python-side arrays from the seed file and saves each
    entity into the appropriate repository on the given ``services`` graph.

    Loads the ``attendance``, ``badges``, and ``friendships`` arrays. All
    datetime strings in the seed file are treated as UTC. Badge award conditions
    are deserialised via ``badges.predicates.predicate_from_dict``.

    Args:
        services: The composed service graph whose repositories will be
            populated. Must expose ``attendance_repo``, ``badge_repo``, and
            ``friendship_repo``.
        seed_path: Path to the seed JSON file. Defaults to
            ``../data/seed.json`` (correct when the working directory is
            ``api-core/``). Pass an absolute path when the caller's working
            directory differs, such as in tests or when spawned as a subprocess.

    Raises:
        FileNotFoundError: If no file exists at ``seed_path``.
        json.JSONDecodeError: If the file does not contain valid JSON.
        KeyError: If a required field is absent from a seed record.
    """
    with open(seed_path, "r") as f:
        jsonData = json.load(f)
    
    attendance_records = jsonData["attendance"]
    badge_records = jsonData["badges"]
    friendship_records = jsonData["friendships"]

    for attendance_record in attendance_records:
        services.attendance_repo.save(
            attendance.Attendance(
                uuid.UUID(attendance_record["attendeeId"]),
                uuid.UUID(attendance_record["eventId"]),
                datetime.datetime.fromisoformat(attendance_record["recordedAt"])
                    .replace(tzinfo=datetime.timezone.utc)
            )
        )

    for badge_record in badge_records:
        award_condition = badges.predicates.predicate_from_dict(
            badge_record["awardCondition"]
        )

        services.badge_repo.save(
            badges.Badge(
                uuid.UUID(badge_record["id"]),
                badge_record["name"],
                badge_record["description"],
                award_condition,
                badge_record["repeatable"]
            )
        )

    for friendship_record in friendship_records:
        services.friendship_repo.save(
            friends.Friendship(
                uuid.UUID(friendship_record["userId"]),
                uuid.UUID(friendship_record["friendId"]),
                datetime.datetime.fromisoformat(friendship_record["createdAt"])
                    .replace(tzinfo=datetime.timezone.utc),
                friends.FriendshipStatus[friendship_record["status"]]
            )
        )