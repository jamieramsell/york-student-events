"""Generic persistence layer for the api-core service.

This package defines a storage-agnostic repository abstraction. Domain slices
(attendance, badges, friends, matching) depend only on the ``IRepository``
interface, never on a concrete backend, so a future database-backed
implementation can be dropped in without changing any caller.
"""

from repositories.base import IEntity, IRepository
from repositories.inmemory import InMemoryRepository

__all__ = ["IEntity", "IRepository", "InMemoryRepository"]
