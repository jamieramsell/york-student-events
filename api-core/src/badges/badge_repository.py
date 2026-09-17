"""In-memory persistence for ``Badge`` entities.

Provides ``InMemoryBadgeRepository``, a dictionary-backed implementation of
``repositories.IRepository`` keyed by the ``uuid.UUID`` IDs of Badge objects.
This stands in for a database-backed repository during early development.
"""
from __future__ import annotations

import typing
import uuid

import sqlalchemy

import repositories
import repositories.sql
from badges import base, predicates


class InMemoryBadgeRepository(
    repositories.InMemoryRepository[uuid.UUID, base.Badge]
):
    """Dictionary backed repository for storing and retrieving Badge entities.
    
    Extends repositories.InMemoryRepository with ``uuid.UUID`` as the managed
    type, providing standard CRUD operations scoped to the UUID keys of Badge
    objects. Used for integration testing before implementing database-backed
    repositories.
    
    See Also:
        repositories.IRepository
        repositories.InMemoryRepository
    """


class SQLAlchemyBadgeRepository(
    repositories.sql.SqlAlchemyRepository[uuid.UUID, base.Badge]
):
    """Database backed repository for storing and retrieving Badge records.
    
    Extends repositories.sql.SqlAlchemyRepository with ``uuid.UUID`` as the
    managed type, providing standard CRUD operations scoped to the keys of Badge
    records. 

    See Also:
        repositories.IRepository
        repositories.sql.SqlAlchemyRepository
    """
    @property
    def _table(self) -> sqlalchemy.Table:
        return repositories.sql.badges
    

    def _id_predicate(
        self, entity_id: uuid.UUID
    ) -> sqlalchemy.ColumnElement[bool]:
        return sqlalchemy.and_(self._table.c.id == entity_id)


    def _to_row(self, entity: base.Badge) -> dict[str, typing.Any]:
        return {
            "id": entity.id,
            "name": entity.name,
            "description": entity.description,
            "award_condition": entity.award_condition.to_dict(),
            "repeatable": entity.repeatable
        }
    

    def _from_row(self, row: sqlalchemy.Row[typing.Any]) -> base.Badge:
        id = row.id
        name = row.name
        description = row.description
        award_condition = predicates.predicate_from_dict(row.award_condition)
        repeatable = row.repeatable

        badge_record = base.Badge(
            id, name, description, award_condition, repeatable
        )
        return badge_record