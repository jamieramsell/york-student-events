"""SQLAlchemy Core persistence backend for the api-core service.

The database-backed half of the ``repositories`` package. Where
``repositories.inmemory`` keeps entities in a dictionary, this subpackage
persists them to a real database via SQLAlchemy Core's expression language, so
the same code runs against Postgres (through psycopg) in production and against
in-memory SQLite in the test suite.

It has two halves:

- ``sql.SqlAlchemyRepository`` — the abstract, ``Engine``-backed base
  implementing ``repositories.IRepository``'s four CRUD methods. Concrete
  per-entity repositories (added in #96) subclass it and supply their backing
  table, their row<->entity mappings, and the WHERE clause that identifies a
  single row.
- ``schema`` — the shared ``MetaData`` and the four ``Table`` definitions the
  service owns (``friendships``, ``badges``, ``awarded_badges``,
  ``attendance``). This one schema is both what the Alembic migrations emit as
  DDL and what the concrete repositories bind to as their table, so the two
  cannot drift apart.

The schema surface is re-exported here for convenience; the repository base is
reached as ``repositories.sql.SqlAlchemyRepository``.

See Also:
    repositories.IRepository
    repositories.inmemory.InMemoryRepository
"""

from repositories.sql.engine import create_engine_from_env
from repositories.sql.schema import (
    attendance,
    awarded_badges,
    badges,
    friendships,
    metadata,
)
from repositories.sql.sql import SqlAlchemyRepository

__all__ = [
    "SqlAlchemyRepository",
    "attendance",
    "awarded_badges",
    "badges",
    "create_engine_from_env",
    "friendships",
    "metadata"
]