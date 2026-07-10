"""Badge slice for the api-core service.

This package models the gamification of the event service for students: earning
badges as they meet new friends, chat, and attend events. It builds on the
generic ``repositories`` abstraction, storing ``Badge`` related records in an
in-memory repository today and remaining open to a database-backed backend
later.

The public surface is the ``Badge`` and ``AwardedBadge`` entities, their
corresponding in-memory repositories, and the service-level operations callers
use to drive badge operations.
"""

from badges.base import Badge, AwardedBadge
from badges.badge_repository import InMemoryBadgeRepository
from badges.awarded_badge_repository import InMemoryAwardedBadgeRepository

__all__ = [
    "Badge",
    "AwardedBadge",
    "InMemoryBadgeRepository",
    "InMemoryAwardedBadgeRepository"
]
