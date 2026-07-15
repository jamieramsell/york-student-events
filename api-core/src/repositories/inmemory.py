import repositories.base as base

class InMemoryRepository(base.IRepository[base.K, base.V]):
    """General-purpose, dictionary backed repository for storing and retrieving
    entities.
    
    Provides concrete implementations of ``repositories.IRepository``'s methods.
    Not intended to be instantiated- this is an abstract class, used as a
    generic common ancestor for all memory-based repositories.
    
    See Also:
        repositories.IRepository
    """

    def __init__(self):
        self.__dict: dict[base.K, base.V] = {}

    def save(self, entity: base.V) -> None:
        self.__dict[entity.get_id()] = entity

    def delete(self, entity_id: base.K) -> None:
        self.__dict.pop(entity_id)

    def find_by_id(
        self, entity_id: base.K
    ) -> base.V | None:
        return self.__dict.get(entity_id)

    def find_all(self) -> list[base.V]:
        return list(self.__dict.values())