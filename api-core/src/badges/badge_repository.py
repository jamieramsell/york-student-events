"""In-memory persistence for ``Badge`` entities.

Provides ``InMemoryBadgeRepository``, a dictionary-backed implementation of
``repositories.IRepository`` keyed by the ``uuid.UUID`` IDs of Badge objects,
plus the package-internal ``_repository`` singleton injected into the service
layer. This stands in for a database-backed repository during early development.
"""

from __future__ import annotations
import base
import repositories
import uuid

class InMemoryBadgeRepository(repositories.IRepository[uuid.UUID]):
    """Dictionary backed repository for storing and retrieving Bagde
    entities.
    
    Extends repositories.IRepository with ``uuid.UUID`` as the managed type,
    providing standard CRUD operations scoped to the UUID keys of Badge objects.
    Used for integration testing before implementing database-backed
    repositories.
    
    See Also:
        repositories.IRepository
    """

    def __init__(self):
        self.__dict: dict[uuid.UUID, base.Badge] = {}

    def save(self, entity: base.Badge) -> None:
        self.__dict[entity.get_id()] = entity

    def delete(self, entity_id: uuid.UUID) -> None:
        self.__dict.pop(entity_id)

    def find_by_id(
        self, entity_id: uuid.UUID
    ) -> base.Badge | None:
        return self.__dict.get(entity_id)

    def find_all(self) -> list[base.Badge]:
        return list(self.__dict.values())

# Variable used to inject an instance of a repository into badge_service.
# Package-internal (single leading underscore): consumed by other modules in the
# friends package, but not part of the package's public API. Do not remove
# unless changing the dependency!
_repository = InMemoryBadgeRepository()