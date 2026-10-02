"""In-memory persistence for ``AwardedBadge`` entities.

Provides ``InMemoryAwardedBadgeRepository``, a dictionary-backed implementation
of ``repositories.IRepository`` keyed by the 2-tuple ``(uuid.UUID, uuid.UUID)``
IDs of AwardedBadge objects. This stands in for a database-backed repository
during early development.
"""
from __future__ import annotations

import typing

import sqlalchemy

import repositories
import repositories.sql
from badges import base


class InMemoryAwardedBadgeRepository(
    repositories.InMemoryRepository[base.AwardId, base.AwardedBadge]
):
    """Dictionary backed repository for storing and retrieving AwardedBadge
    records.
    
    Extends repositories.InMemoryRepository with the 2-tuple
    ``(uuid.UUID, uuid.UUID)`` as the managed type, providing standard CRUD
    operations scoped to the keys of AwardedBadge objects. Used for integration
    testing before implementing database-backed repositories.
    
    See Also:
        repositories.IRepository
        repositories.InMemoryRepository
    """


class SQLAlchemyAwardedBadgeRepository(
    repositories.sql.SqlAlchemyRepository[base.AwardId, base.AwardedBadge]
):
    """Database backed repository for storing and retrieving AwardedBadge
    records.
    
    Extends repositories.sql.SqlAlchemyRepository with
    ``tuple[uuid.UUID, uuid.UUID]`` as the managed type, providing standard CRUD
    operations scoped to the keys of AwardedBadge records. 

    See Also:
        repositories.IRepository
        repositories.sql.SqlAlchemyRepository
    """
    @property
    def _table(self) -> sqlalchemy.Table:
        return repositories.sql.awarded_badges
    

    def _id_predicate(
        self, entity_id: base.AwardId
    ) -> sqlalchemy.ColumnElement[bool]:
        return sqlalchemy.and_(self._table.c.user_id == entity_id[0],
                               self._table.c.badge_id == entity_id[1])


    def _to_row(self, entity: base.AwardedBadge) -> dict[str, typing.Any]:
        return {
            "user_id": entity.user_id,
            "badge_id": entity.badge_id,
            "awarded_at": entity.awarded_at,
            "times_awarded": entity.times_awarded
        }
    

    def _from_row(self, row: sqlalchemy.Row[typing.Any]) -> base.AwardedBadge:
        user_id = row.user_id
        badge_id = row.badge_id
        awarded_at = row.awarded_at
        times_awarded = row.times_awarded

        award_record = base.AwardedBadge(
            user_id, badge_id, awarded_at, times_awarded
        )
        return award_record