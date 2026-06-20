"""The generic repository interface.

This mirrors the ``IRepository<T>`` abstraction used by the Java
``event-service``: a small, storage-agnostic CRUD contract that every domain
slice can depend on without knowing how (or where) entities are actually
persisted. Concrete backends — in-memory today, a real database later —
implement this interface and remain interchangeable.

The interface is parameterised by the *key* type ``K`` alone, over
``IEntity[K]``. This guarantees the lookup key type and the entity's own id
type cannot drift apart — the gap Python's type system leaves open if both the
key and entity types are free parameters (it has no F-bounded polymorphism, so
``IRepository<ID, T extends IEntity<ID>>`` from the Java side cannot be
expressed directly). Concrete repositories recover their precise entity type by
narrowing the return types of the overridden methods; see the example below.

Stdlib only, in keeping with the project's "no third-party dependencies
without a requirements.txt" convention.
"""

# This import allows type annotations to be used which haven't been defined by
# runtime, avoiding import cycles. Do not remove!
from __future__ import annotations

import abc
import typing

# K parameterises both IEntity and IRepository, so it must be defined first.
K = typing.TypeVar("K")


class IEntity(abc.ABC, typing.Generic[K]):
    """Represents an entity which can be stored in a repository.

    This common interface enforces that all entities have an ID which can be
    used as a key. Due to the nature of the interfaces used by api-core, these
    IDs may not be UUIDs, therefore the ID returned is simply of some generic
    type; what the ID consists of is to be implemented by individual concrete
    classes.

    Args:
        K: the type of ID used by this entity.

    See Also:
        IRepository
    """

    @abc.abstractmethod
    def get_id(self) -> K:
        """Retrieves the unique identifier of this entity."""


class IRepository(abc.ABC, typing.Generic[K]):
    """Generic repository interface providing standard CRUD operations.

    All domain-specific repository interfaces should extend this interface,
    binding the key type and narrowing the entity return types to their
    concrete entity. For example::

        class BadgeRepository(IRepository[int]):
            def find_by_id(self, entity_id: int) -> Badge | None: ...
            def find_all(self) -> list[Badge]: ...

    Narrowing an overridden method's return type to a subtype is permitted, so
    callers of ``BadgeRepository`` get ``Badge`` back while the base contract
    keeps the key and entity id types provably aligned.

    Args:
        K: the type of ID used to key entities in this repository.
    """

    @abc.abstractmethod
    def save(self, entity: IEntity[K]) -> None:
        """Saves an entity to the repository.

        If an entity with the same ID already exists, it is overwritten.

        Args:
            entity: the entity to save; must not be None.
        """

    @abc.abstractmethod
    def delete(self, entity_id: K) -> None:
        """Removes the entity with the given ID from the repository.

        Args:
            entity_id: the ID of the entity to remove.

        Raises:
            KeyError: if no entity with the given ID exists.
        """

    @abc.abstractmethod
    def find_by_id(self, entity_id: K) -> IEntity[K] | None:
        """Looks up an entity by its ID.

        Args:
            entity_id: the ID of the entity to retrieve.

        Returns:
            The entity, if one exists, or None.
        """

    @abc.abstractmethod
    def find_all(self) -> list[IEntity[K]]:
        """Retrieves all entities currently held in the repository.

        Returns:
            A list of all entities; never None, but may be empty.
        """
