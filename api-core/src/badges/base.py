"""Core domain model for the Badge slice.

Defines the ``Badge`` entity, and the Predicate type, which outlines the award
condition of a given badge. This is the lowest layer of the badge package: it 
depends only on the generic ``repositories`` abstraction, and is consumed by
both the repository and service layers.
"""

import badges.predicates as predicates
import dataclasses
import datetime
import repositories
import uuid


@dataclasses.dataclass(frozen = True, eq = False)
class Badge(repositories.IEntity[uuid.UUID]):
    """
    Defines the core structure of a Badge in the ``api-core`` service.

    Args:
        id: The ID of the badge; must not be None.
        name: The display name of the Badge; must not be None.
        description: An optional extra description of the Badge.
        award_condition: A predicate, representing the condition(s) which must 
            be fulfilled in order to award the badge to a user.
        repeatable: Represents whether the badge can be earned more than once by
            any given user.
    """
    id: uuid.UUID
    name: str
    description: str | None
    award_condition: predicates.IPredicate
    repeatable: bool = False


    def get_id(self) -> uuid.UUID:
        return self.id


    def __eq__(self, other: object) -> bool:
        if not isinstance(other, Badge):
            return False
        
        return other.id == self.id


    def __hash__(self) -> int:
        return self.id.__hash__()
    

# Ordered tuple, representing the ID of an AwardedBadge record, which stores
# the Student's user ID, followed by the ID of the badge they've earned.
type AwardId = tuple[uuid.UUID, uuid.UUID]


def _generate_award_id(user_id: uuid.UUID, badge_id: uuid.UUID) -> AwardId:
    """Convenience function used to generate the ID of a given AwardedBadge
    record.
    """
    return (user_id, badge_id)


@dataclasses.dataclass(frozen = True)
class AwardedBadge(repositories.IEntity[AwardId]):
    """Defines the structure of the record of a Badge that has been awarded to
    a user.
    
    Re-awards keep one record per (user, badge): a new frozen instance with
    times_awarded + 1 replaces the old one.

    Args:
        user_id: The ID of the user who earned the badge.
        badge_id: The ID of the badge in question.
        awarded_at: The datetime of the most recent badge award.
        times_awarded: Represents how many times the user has earned this badge.
            Defaults to 1.
    """
    user_id: uuid.UUID
    badge_id: uuid.UUID
    awarded_at: datetime.datetime
    times_awarded: int = 1
    

    def get_id(self) -> AwardId:
        return _generate_award_id(self.user_id, self.badge_id)