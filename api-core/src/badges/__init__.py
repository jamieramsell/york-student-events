"""Badge slice for the api-core service.

This package models the gamification of the event service for students: earning
badges as they meet new friends, chat, and attend events. It builds on the
generic ``repositories`` abstraction, storing ``Badge`` related records in an
in-memory repository today and remaining open to a database-backed backend
later.

The public surface is the ``Badge`` and ``AwardedBadge`` entities, the
``AwardContext`` (with its ``AttendedEvent`` member) that
``BadgeService.evaluate_badges`` reads, their corresponding in-memory
repositories, ``BadgeService`` (the operations callers use to drive badge
operations) and ``EvaluationService`` (the ``activity``-driven auto-award
listener).

The composable predicate vocabulary (``IPredicate`` and its combinators/leaves,
plus ``predicate_from_dict``) is intentionally left behind ``badges.predicates``
rather than re-exported here.
"""
from badges.awarded_badge_repository import InMemoryAwardedBadgeRepository
from badges.badge_repository import InMemoryBadgeRepository
from badges.badge_service import (
    AwardedBadgeRepository,
    BadgeRepository,
    BadgeService,
)
from badges.base import AwardedBadge, Badge
from badges.evaluation import EvaluationService
from badges.predicates import AttendedEvent, AwardContext

__all__ = [
    "Badge",
    "AwardedBadge",
    "AwardContext",
    "AttendedEvent",
    "InMemoryBadgeRepository",
    "InMemoryAwardedBadgeRepository",
    "BadgeService",
    "BadgeRepository", 
    "AwardedBadgeRepository",
    "EvaluationService"
]
