"""In-memory persistence for ``AwardedBadge`` entities.

Provides ``InMemoryAwardedBadgeRepository``, a dictionary-backed implementation
of ``repositories.IRepository`` keyed by the 2-tuple ``(uuid.UUID, uuid.UUID)``
IDs of AwardedBadge objects, plus the package-internal ``_repository`` singleton
which gets injected into the service layer. This stands in for a database-backed
repository during early development.
"""

from __future__ import annotations
import badges.base as base
import repositories

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

# Variable used to inject an instance of a repository into badge_service.
# Package-internal (single leading underscore): consumed by other modules in the
# badges package, but not part of the package's public API. Do not remove
# unless changing the dependency!
_repository = InMemoryAwardedBadgeRepository()