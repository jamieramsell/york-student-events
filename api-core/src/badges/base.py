"""Core domain model for the Badge slice.

Defines the ``Badge`` entity, and the Predicate type, which outlines the award
condition of a given badge. This is the lowest layer of the badge package: it 
depends only on the generic ``repositories`` abstraction, and is consumed by
both the repository and service layers.
"""

import dataclasses
import datetime
import predicates
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