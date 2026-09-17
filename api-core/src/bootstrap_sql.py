"""Database-backed composition entry point for the api-core service.

Wraps the shared composition root (``bootstrap.bootstrap``) with the one thing
it does not supply itself: concrete SQLAlchemy repositories over the shared
PostgreSQL database. Where ``bootstrap`` defaults every repository to a fresh
in-memory implementation, and the bridge responder seeds it with canned
in-memory data, this module injects the ``SQLAlchemy*Repository`` backends, so
the composed graph reads and writes genuinely persisted state.

The public surface is ``bootstrap_sql()``.

See Also:
    bootstrap.bootstrap
    repositories.sql
"""
import attendance
import badges
import bootstrap
import friends
import repositories.sql


def bootstrap_sql() -> bootstrap.Services:
    """Compose the api-core service graph over the SQLAlchemy repositories.

    The database-backed counterpart to the responder's canned wiring, which
    builds a single ``Engine`` from the ``DATABASE_URL`` environment variable,
    constructs the four ``SQLAlchemy*Repository`` backends over that one engine,
    and hands them to the shared ``bootstrap`` composition root. With every
    repository supplied, the services persist to and read from Postgres, rather
    than an in-memory dictionary; their public signatures and behaviour are
    unchanged (the repository-swap principle).

    ``register`` is left at its ``bootstrap`` default of ``True``, so
    ``evaluation_service`` is subscribed to the ``activity`` registry and
    automatic badge evaluation is live. This is appropriate for a real, stateful
    entry point, unlike the stateless bridge responder, which passes
    ``register=False``.

    Returns:
        A ``Services`` container holding the four SQLAlchemy repositories and
        the fully wired services, with ``evaluation_service`` subscribed to
        ``activity``.

    Raises:
        ValueError: if the ``DATABASE_URL`` environment variable is not set.
    """

    engine = repositories.sql.create_engine_from_env()   # reads DATABASE_URL
    services = bootstrap.bootstrap(
        attendance_repository=attendance.SQLAlchemyAttendanceRepository(engine),
        friendship_repository=friends.SQLAlchemyFriendshipRepository(engine),
        badge_repository=badges.SQLAlchemyBadgeRepository(engine),
        awarded_badge_repository=badges.SQLAlchemyAwardedBadgeRepository(engine)
    )

    return services