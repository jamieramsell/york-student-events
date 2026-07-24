"""Shared table metadata for the api-core persistence layer.

A single ``MetaData`` holding the four tables this service owns:
``friendships``, ``badges``, ``awarded_badges`` and ``attendance``.

This is the one source of truth for the api-core schema, as the Alembic
migrations target this ``metadata`` to emit DDL, and the concrete SQLAlchemy
repositories (added in #96) reference these ``Table`` objects as their
``_table`` hook. Defining the schema once here keeps the migration and the
repositories from drifting apart.

These tables are api-core's alone. Columns that reference users or events
(``user_id``, ``attendee_id``, ``event_id``) point at rows owned by
event-service, so they carry no cross-service foreign key. That data is instead
exchanged over the subprocess bridge, not by joining across service boundaries.

See Also:
    repositories.sql.SqlAlchemyRepository
"""

from __future__ import annotations

import datetime

import sqlalchemy

# One registry for every api-core table; handed to Alembic as target_metadata.
metadata = sqlalchemy.MetaData()


# Type decorator for datetime values which guarantees timezone-aware UTC
class UtcDateTime(sqlalchemy.types.TypeDecorator[datetime.datetime]):
    """A DateTime that guarantees timezone-aware UTC on both read and write.

    SQLite has no tz-aware column type and returns naive datetimes; this
    reattaches UTC on read so the domain layer always sees aware values,
    matching Postgres timestamptz. Writes are normalised to UTC so the stored
    wall-clock is unambiguous.
    """
    impl = sqlalchemy.DateTime(timezone=True)
    cache_ok = True

    def process_bind_param(
        self,
        value: datetime.datetime | None,
        dialect: sqlalchemy.Dialect
    ) -> datetime.datetime | None:
        if value is not None:
            if value.tzinfo is None:
                raise ValueError("value (the datetime provided) must be"
                                 + " timezone aware.")
            
            value = value.astimezone(datetime.timezone.utc)

        return value

    def process_result_value(
        self,
        value: datetime.datetime | None,
        dialect: sqlalchemy.Dialect
    ) -> datetime.datetime | None:
        if value is not None and value.tzinfo is None:
            value = value.replace(tzinfo=datetime.timezone.utc)
        return value
    

# A pending or accepted link between two users. The (user_id, friend_id) pair is
# the composite key, whose order-independence (a Friendship's id is a
# frozenset) is enforced by the repository layer in #96, not by the schema.
friendships = sqlalchemy.Table(
    "friendships",
    metadata,
    sqlalchemy.Column("user_id", sqlalchemy.Uuid, primary_key=True),
    sqlalchemy.Column("friend_id", sqlalchemy.Uuid, primary_key=True),
    sqlalchemy.Column("created_at", UtcDateTime, nullable=False),
    # Stores the FriendshipStatus value ("pending" / "accepted").
    sqlalchemy.Column("status", sqlalchemy.String(16), nullable=False),
)


# A badge definition. award_condition holds the serialised predicate DSL (the
# shape produced by badges.predicates.predicate_from_dict).
badges = sqlalchemy.Table(
    "badges",
    metadata,
    sqlalchemy.Column("id", sqlalchemy.Uuid, primary_key=True),
    sqlalchemy.Column("name", sqlalchemy.String, nullable=False),
    sqlalchemy.Column("description", sqlalchemy.String, nullable=True),
    sqlalchemy.Column("award_condition", sqlalchemy.JSON, nullable=False),
    # Application-supplied; defaults to False on the entity.
    sqlalchemy.Column("repeatable", sqlalchemy.Boolean, nullable=False),
)


# One row per (user, badge) award. Re-awards bump times_awarded in place rather
# than inserting a second row. badge_id is a same-service reference, so it may
# carry a foreign key; user_id is event-service-owned and deliberately does not.
awarded_badges = sqlalchemy.Table(
    "awarded_badges",
    metadata,
    sqlalchemy.Column("user_id", sqlalchemy.Uuid, primary_key=True),
    sqlalchemy.Column(
        "badge_id",
        sqlalchemy.Uuid,
        sqlalchemy.ForeignKey("badges.id"),
        primary_key=True,
    ),
    sqlalchemy.Column("awarded_at", UtcDateTime, nullable=False),
    # Application-supplied; defaults to 1 on the entity.
    sqlalchemy.Column("times_awarded", sqlalchemy.Integer, nullable=False),
)


# A record that a student attended an event. Both ids are event-service-owned,
# so the (attendee_id, event_id) composite key carries no foreign keys.
attendance = sqlalchemy.Table(
    "attendance",
    metadata,
    sqlalchemy.Column("attendee_id", sqlalchemy.Uuid, primary_key=True),
    sqlalchemy.Column("event_id", sqlalchemy.Uuid, primary_key=True),
    sqlalchemy.Column("recorded_at", UtcDateTime, nullable=False),
)