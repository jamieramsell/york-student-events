"""Badge slice for the api-core service.

This package models the gamification of the event service for students: earning
badges as they meet new friends, chat, and attend events. It builds on the
generic ``repositories`` abstraction, storing ``Badge`` related records in an
in-memory repository today and remaining open to a database-backed backend
later.

The public surface is the ``Badge`` and ``AwardedBadge`` entities, the
``AwardContext`` (with its ``AttendedEvent`` member) that ``evaluate_badges``
reads, their corresponding in-memory repositories, and the service-level
operations callers use to drive badge operations.

The composable predicate vocabulary (``IPredicate`` and its combinators/leaves,
plus ``predicate_from_dict``) is intentionally left behind ``badges.predicates``
rather than re-exported here.
"""

from badges.awarded_badge_repository import InMemoryAwardedBadgeRepository
from badges.base import Badge, AwardedBadge
from badges.predicates import AwardContext, AttendedEvent
from badges.badge_repository import InMemoryBadgeRepository
from badges.awarded_badge_repository import InMemoryAwardedBadgeRepository
from badges.badge_service import (
    award_badge,
    create_badge,
    evaluate_badges,
    get_user_badges,
    has_badge,
    revoke_badge,
)
from badges.evaluation import (
    build_award_context,
    register
)
from badges.predicates import AwardContext, AttendedEvent

__all__ = [
    "Badge",
    "AwardedBadge",
    "AwardContext",
    "AttendedEvent",
    "InMemoryBadgeRepository",
    "InMemoryAwardedBadgeRepository",
    "award_badge",
    "create_badge",
    "evaluate_badges",
    "get_user_badges",
    "has_badge",
    "revoke_badge",
    "build_award_context",
    "register"
]

# Call badges.evaluation.register() on module import to automatically subscribe
# the evaluation listener to be notified of any changes to user data.
register()
