"""In-memory persistence for ``Friendship`` entities.

Provides ``InMemoryFriendshipRepository``, a dictionary-backed implementation of
``repositories.IRepository``, and ``SQLAlchemyFriendshipRepository``, which are
keyed by the frozenset IDs of Friendship objects.
"""
from __future__ import annotations

import typing

import sqlalchemy

import repositories
import repositories.sql
from friends import base


class InMemoryFriendshipRepository(
    repositories.InMemoryRepository[base.FriendshipId, base.Friendship]
):
    """Dictionary backed repository for storing and retrieving Friendship
    entities.
    
    Extends repositories.InMemoryRepository with frozenset[uuid.UUID] as the
    managed type, providing standard CRUD operations scoped to the frozenset
    keys of Friendship objects. Used for testing purposes only.
    
    See Also:
        repositories.IRepository
        repositories.InMemoryRepository
    """


class SQLAlchemyFriendshipRepository(
    repositories.sql.SqlAlchemyRepository[base.FriendshipId, base.Friendship]
):
    """Database backed repository for storing and retrieving Friendship
    records.
    
    Extends repositories.sql.SqlAlchemyRepository with ``frozenset[uuid.UUID]``
    as the managed type, providing standard CRUD operations scoped to the keys
    (formed of a frozenset of UUIDs) of Friendship records. 

    See Also:
        repositories.IRepository
        repositories.sql.SqlAlchemyRepository
    """
    @property
    def _table(self) -> sqlalchemy.Table:
        return repositories.sql.friendships
    

    def _id_predicate(
        self, entity_id: base.FriendshipId
    ) -> sqlalchemy.ColumnElement[bool]:
        # The order of the two user IDs within a friendship record is arbitrary,
        # so we must compare the IDs to the database using both possible
        # orderings.
        user1_id, user2_id = entity_id
        return sqlalchemy.or_(
            sqlalchemy.and_(
                self._table.c.user_id == user1_id,
                self._table.c.friend_id == user2_id
            ),
            sqlalchemy.and_(
                self._table.c.user_id == user2_id,
                self._table.c.friend_id == user1_id
            )
        )


    def _to_row(self, entity: base.Friendship) -> dict[str, typing.Any]:
        return {
            "user_id": entity.user_id,
            "friend_id": entity.friend_id,
            "created_at": entity.created_at,
            "status": entity.friendship_status.value
        }
    

    def _from_row(self, row: sqlalchemy.Row[typing.Any]) -> base.Friendship:
        user_id = row.user_id
        friend_id = row.friend_id
        created_at = row.created_at

        match row.status:
            case base.FriendshipStatus.ACCEPTED.value:
                status = base.FriendshipStatus.ACCEPTED
            case base.FriendshipStatus.PENDING.value:
                status = base.FriendshipStatus.PENDING
            case _:
                raise ValueError("Invalid status value provided. Status must be"
                                 + " one of the values of the FriendshipStatus"
                                 + " constants.")

        friendship_record = base.Friendship(
            user_id, friend_id, created_at, status
        )
        return friendship_record