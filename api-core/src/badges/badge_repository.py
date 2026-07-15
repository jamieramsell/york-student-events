"""In-memory persistence for ``Badge`` entities.

Provides ``InMemoryBadgeRepository``, a dictionary-backed implementation of
``repositories.IRepository`` keyed by the ``uuid.UUID`` IDs of Badge objects,
plus the package-internal ``_repository`` singleton injected into the service
layer. This stands in for a database-backed repository during early development.
"""

from __future__ import annotations
import badges.base as base
import repositories
import uuid

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

# Variable used to inject an instance of a repository into badge_service.
# Package-internal (single leading underscore): consumed by other modules in the
# badges package, but not part of the package's public API. Do not remove
# unless changing the dependency!
_repository = InMemoryBadgeRepository()