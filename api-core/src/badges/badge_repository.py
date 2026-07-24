"""In-memory persistence for ``Badge`` entities.

Provides ``InMemoryBadgeRepository``, a dictionary-backed implementation of
``repositories.IRepository`` keyed by the ``uuid.UUID`` IDs of Badge objects.
This stands in for a database-backed repository during early development.
"""
from __future__ import annotations

import uuid

import repositories

import badges.base as base


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