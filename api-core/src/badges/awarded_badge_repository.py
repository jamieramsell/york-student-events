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
    repositories.IRepository[base.AwardId, base.AwardedBadge]
):
    """Dictionary backed repository for storing and retrieving AwardedBadge
    records.
    
    Extends repositories.IRepository with the 2-tuple ``(uuid.UUID, uuid.UUID)``
    as the managed type, providing standard CRUD operations scoped to the keys
    of AwardedBadge objects. Used for integration testing before implementing
    database-backed repositories.
    
    See Also:
        repositories.IRepository
    """

    def __init__(self):
        self.__dict: dict[base.AwardId, base.AwardedBadge] = {}


    def save(self, entity: base.AwardedBadge) -> None:
        self.__dict[entity.get_id()] = entity


    def delete(self, entity_id: base.AwardId) -> None:
        self.__dict.pop(entity_id)


    def find_by_id(
        self, entity_id: base.AwardId
    ) -> base.AwardedBadge | None:
        return self.__dict.get(entity_id)


    def find_all(self) -> list[base.AwardedBadge]:
        return list(self.__dict.values())

# Variable used to inject an instance of a repository into badge_service.
# Package-internal (single leading underscore): consumed by other modules in the
# badges package, but not part of the package's public API. Do not remove
# unless changing the dependency!
_repository = InMemoryAwardedBadgeRepository()