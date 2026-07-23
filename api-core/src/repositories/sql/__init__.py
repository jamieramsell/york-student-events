from repositories.sql.schema import (
    metadata,
    friendships,
    badges,
    awarded_badges,
    attendance
)
from repositories.sql.sql import SqlAlchemyRepository

__all__ = [
    "SqlAlchemyRepository",
    "metadata",
    "friendships",
    "badges",
    "awarded_badges",
    "attendance"
]