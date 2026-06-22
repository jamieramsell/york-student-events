"""In-memory persistence for ``Friendship`` entities.

Provides ``InMemoryFriendshipRepository``, a dictionary-backed implementation of
``repositories.IRepository`` keyed by the frozenset IDs of Friendship objects,
plus the package-internal ``_repository`` singleton injected into the service
layer. This stands in for a database-backed repository during early development.
"""

import base
import repositories

class InMemoryFriendshipRepository(repositories.IRepository[frozenset]):
    """Dictionary backed repository for storing and retrieving Friendship
    entities.
    
    Extends repositories.IRepository with frozenset as the managed type,
    providing standard CRUD operations scoped to the frozenset keys of
    Friendship objects. Used for integration testing before implementing
    database-backed repositories.
    
    See Also:
        repositories.IRepository
    """

    def __init__(self):
        self.__dict = {}

    def save(self, entity: base.Friendship) -> None:
        self.__dict[entity.get_id()] = entity

    def delete(self, entity_id: frozenset) -> None:
        self.__dict.pop(entity_id)

    def find_by_id(
        self, entity_id: frozenset
    ) -> base.Friendship | None:
        return self.__dict.get(entity_id)

    def find_all(self) -> list[base.Friendship]:
        return list(self.__dict.values())

# Variable used to inject an instance of a repository into friendship_service.
# Package-internal (single leading underscore): consumed by other modules in the
# friends package, but not part of the package's public API. Do not remove
# unless changing the dependency!
_repository = InMemoryFriendshipRepository()