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
V = typing.TypeVar("V", bound="IEntity[typing.Any]")

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


def _declared_key_type(entity_type: type) -> object | None:
    """Finds the key type of an entity class passed to ``IEntity[...]``.

    Scans the entity's method resolution order so the ``IEntity`` base is
    found even when it is inherited indirectly.

    Args:
        entity_type: the concrete entity class to inspect.

    Returns:
        The key type argument, or None if the class never parameterises
        ``IEntity``.
    """
    for ancestor in getattr(entity_type, "__mro__", ()):
        for parameterised_base in getattr(ancestor, "__orig_bases__", ()):
            unsubscripted = typing.get_origin(parameterised_base)
            if unsubscripted is not None and issubclass(unsubscripted, IEntity):
                type_args = typing.get_args(parameterised_base)
                if type_args:
                    return type_args[0]
    return None


class IRepository(abc.ABC, typing.Generic[K, V]):
    """Generic repository interface providing standard CRUD operations.

    All domain-specific repository interfaces should extend this interface,
    binding the key type and narrowing the entity return types to their
    concrete entity. For example::

        class BadgeRepository(IRepository[uuid.UUID, Badge]):
            def find_by_id(self, entity_id: uuid.UUID) -> Badge | None: ...
            def find_all(self) -> list[Badge]: ...

    Narrowing an overridden method's return type to a subtype is permitted, so
    callers of ``BadgeRepository`` get ``Badge`` back while the base contract
    keeps the key and entity id types provably aligned.

    Args:
        K: the type of ID used to key entities in this repository.
        V: the type of Entity stored within this repository

    Raises:
        TypeError:
            if the declared key type of the repository does not match that of
            the declared type of entity that the repository is defined to store.
    """

    # Called whenever a subclass is initialised. Checks whether K is equal to
    # the type of key of V.
    def __init_subclass__(cls, **kwargs: typing.Any) -> None:
        super().__init_subclass__(**kwargs)

        # For each base (parent) of the class:
        for declared_base in getattr(cls, "__orig_bases__", ()):

            # This declared base isn't a parameterised IRepository (e.g. it's a
            # mixin or plain ABC listed alongside it), so try the next base.
            if typing.get_origin(declared_base) is not IRepository:
                continue
            
            # Defensive: confirm exactly two type arguments were supplied.
            # (Given the origin check above, this practically always holds.)
            type_args = typing.get_args(declared_base)
            if len(type_args) != 2:
                continue

            # Skip intermediate generic subclasses whose arguments are still
            # unbound type parameters rather than concrete types.
            repository_key_type, entity_type = type_args
            
            if (isinstance(repository_key_type, typing.TypeVar)
                or isinstance(entity_type, typing.TypeVar)
            ):
                continue
            
            # Find the key type of the entity
            entity_key_type = _declared_key_type(entity_type)

            # If the key type of the entity does not match the declared key type
            # of the repository, raise an error
            if (entity_key_type is not None
                and entity_key_type != repository_key_type
            ):
                raise TypeError(
                    f"{cls.__name__}: repository key {repository_key_type!r}" 
                    + f" does not match {entity_type.__name__}'s entity key"
                    + f" {entity_key_type!r}"
                )


    @abc.abstractmethod
    def save(self, entity: V) -> None:
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
    def find_by_id(self, entity_id: K) -> V | None:
        """Looks up an entity by its ID.

        Args:
            entity_id: the ID of the entity to retrieve.

        Returns:
            The entity, if one exists, or None.
        """


    @abc.abstractmethod
    def find_all(self) -> list[V]:
        """Retrieves all entities currently held in the repository.

        Returns:
            A list of all entities; never None, but may be empty.
        """
