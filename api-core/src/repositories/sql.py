"""SQLAlchemy Core backend for the generic repository interface.

The database-backed counterpart to ``InMemoryRepository``: a general-purpose,
``Engine``-backed base implementing ``repositories.IRepository``'s four CRUD
methods with Core's expression language, so the same code runs against Postgres
(via psycopg) in production and in-memory SQLite in the test suite. Not intended
to be instantiated directly, as concrete per-entity repositories subclass it and
supply their table and row<->entity mapping.

See Also:
    repositories.IRepository
    repositories.inmemory.InMemoryRepository
"""

from __future__ import annotations

import abc
import repositories.base as base
import sqlalchemy
import typing

class SqlAlchemyRepository(base.IRepository[base.K, base.V]):
    """Abstract ``Engine``-backed repository built on SQLAlchemy Core.

    Subclasses bind ``K``/``V`` to their concrete key and entity types and
    implement three hooks: the backing ``Table`` and the two mappings between a
    row and a domain entity. The CRUD methods here are dialect-portable, so a
    subclass works unchanged on Postgres and SQLite.

    Args:
        engine: the SQLAlchemy ``Engine`` this repository issues statements on.
    """
    def __init__(self, engine: sqlalchemy.Engine) -> None:
        self._engine = engine


    @property
    @abc.abstractmethod
    def _table(self) -> sqlalchemy.Table:
        """The table this repository reads from and writes to."""


    @property
    def _primary_key_column(self) -> sqlalchemy.Column[typing.Any]:
        """The single primary-key column, used to key by entity id."""
        (pk,) = self._table.primary_key.columns
        return pk
    

    @abc.abstractmethod
    def _to_row(self, entity: base.V) -> dict[str, typing.Any]:
        """Flattens an entity into a column-name -> value mapping."""


    @abc.abstractmethod
    def _from_row(self, row: sqlalchemy.Row[typing.Any]) -> base.V:
        """Reconstructs an entity from a result row."""


    def save(self, entity: base.V) -> None:
        pk = self._primary_key_column

        # Delete (if a record with the given id already exists) and insert back
        # into the table within one transaction, so that if the insertion fails,
        # the deletion is rolled back.
        with self._engine.begin() as conn:
            conn.execute(self._table.delete().where(pk == entity.get_id()))
            conn.execute(self._table.insert().values(self._to_row(entity)))


    def delete(self, entity_id: base.K) -> None:
        pk = self._primary_key_column

        with self._engine.begin() as conn:
            result = conn.execute(self._table.delete().where(pk == entity_id))
            if result.rowcount == 0:
                raise KeyError(entity_id)


    def find_by_id(self, entity_id: base.K) -> base.V | None:
        pk = self._primary_key_column

        with self._engine.begin() as conn:
            row = conn.execute(
                self._table.select().where(pk == entity_id)
            ).one_or_none()
            return self._from_row(row) if row is not None else None


    def find_all(self) -> list[base.V]:
        with self._engine.connect() as conn:
            rows = conn.execute(self._table.select()).all()

        return [self._from_row(row) for row in rows]
    