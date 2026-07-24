"""In-memory persistence for ``AwardedBadge`` entities.

Provides ``InMemoryAwardedBadgeRepository``, a dictionary-backed implementation
of ``repositories.IRepository`` keyed by the 2-tuple ``(uuid.UUID, uuid.UUID)``
IDs of AwardedBadge objects. This stands in for a database-backed repository
during early development.
"""
from __future__ import annotations

import repositories
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