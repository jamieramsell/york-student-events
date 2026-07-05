import dataclasses
import collections.abc
import repositories
import typing
import uuid

# Define Predicate type alias, representing a callable, taking kwargs input,
# and returning a boolean value
type Predicate = collections.abc.Callable[[dict[typing.Any, typing.Any]], bool]

@dataclasses.dataclass(frozen = True)
class Badge(repositories.IEntity[uuid.UUID]):
    """
    Defines the core structure of a Badge in the ``api-core`` service.

    Args:
        id: The ID of the badge; must not be None.
        name: The display name of the Badge; must not be None.
        description: An optional extra description of the Badge.
        award_condition: A predicate which represents the condition(s) which
            must be fulfilled in order to award the badge to a user
    """
    id: uuid.UUID
    name: str
    description: str | None
    award_condition: Predicate

    def get_id(self) -> uuid.UUID:
        return self.id