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