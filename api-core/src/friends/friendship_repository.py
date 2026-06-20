import repositories
import friendship_service

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

    def save(self, entity: friendship_service.Friendship) -> None:
        self.__dict[entity.get_id()] = entity

    def delete(self, entity_id: frozenset) -> None:
        self.__dict.pop(entity_id)

    def find_by_id(
        self, entity_id: frozenset
    ) -> friendship_service.Friendship | None:
        return self.__dict.get(entity_id)

    def find_all(self) -> list[friendship_service.Friendship]:
        return self.__dict.values()