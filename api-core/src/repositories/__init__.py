"""Generic persistence layer for the api-core service.

This package defines a storage-agnostic repository abstraction. Domain slices
(attendance, badges, friends, recommendations) should only depend on the
``IRepository`` interface, never on a concrete backend, meaning that a future
database-backed implementation can be dropped in without changing any caller.
"""

from repositories.base import IEntity, IRepository
from repositories.inmemory import InMemoryRepository

__all__ = ["IEntity", "IRepository", "InMemoryRepository"]
