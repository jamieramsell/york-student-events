"""Service-level operations for the badges slice.

Exposes the badge use cases callers depend on: creating badges, manually
awarding and revoking them, querying whether (and how many times) a user holds a
badge, and the condition-driven ``evaluate_badges`` that auto-awards eligible
badges from an ``AwardContext``. Orchestrates the ``Badge`` / ``AwardedBadge``
domain models from ``base`` with their in-memory repositories, keeping
persistence details out of callers.
"""

import badges.awarded_badge_repository as awarded_badge_repository
import badges.base as base
import badges.predicates as predicates
import badges.badge_repository as badge_repository
import datetime
import uuid


def create_badge(
    name: str,
    description: str | None,
    award_condition: predicates.IPredicate,
    repeatable: bool = False
) -> base.Badge:
    """Creates a badge with the given specification and saves it to the badge
    repository.
    
    Args:
        name: The name of the Badge
        description: An optional extended description for the Badge
        award_condition: The Predicate which must be met in order to award the
            Badge automatically
        repeatable: Whether the Badge can be earned multiple times

    Returns:
        The Badge object that has been created and saved to the Badge
        repository.
    """
    badge_id = uuid.uuid4()
    badge = base.Badge(badge_id, name, description, award_condition, repeatable)
    badge_repository._repository.save(badge)

    return badge


def award_badge(user_id: uuid.UUID, badge_id: uuid.UUID) -> None:
    """Attempts to manually award a badge to a User.

    If the user hasn't previously earned the badge, then the method saves a new
    ``AwardedBadge(times_awarded=1)`` record to the AwardedBadge repository. If
    however there is a pre-existing award, and ``badge.repeatable == True``,
    then the existing award is overwritten, with ``times_awarded + 1``, and a
    fresh ``awarded_at`` timestamp.
    
    Args:
        user_id: The ID of the User to award the Badge to.
        badge_id: The Badge's Id

    Raises:
        ValueError: if the given Badge does not exist.
        ValueError: if an AwardedBadge record for the user already exists, and
            the badge is not repeatable.
    """
    # Data retrieval & validation
    badge = badge_repository._repository.find_by_id(badge_id)
    if badge is None:
        raise ValueError("The given badge does not exist.")
    
    award_id = base._generate_award_id(user_id, badge_id)
    existing_award = awarded_badge_repository._repository.find_by_id(award_id)

    times_awarded = 1

    if existing_award is not None:
        if not badge.repeatable:
            raise ValueError("An AwardedBadge record for the user already"
                + " exists, but the badge is not repeatable")
        
        times_awarded += existing_award.times_awarded
    
    # Construct and save the awarded badge record
    new_award = base.AwardedBadge(
        user_id,
        badge_id,
        datetime.datetime.now(),
        times_awarded
    )

    awarded_badge_repository._repository.save(new_award)


def revoke_badge(user_id: uuid.UUID, badge_id: uuid.UUID) -> None:
    """Manually removes an AwardedBadge record from a given User.

    If the user has earned the Badge multiple times, the entire record is
    removed, meaning they will no longer have a single instance of the Badge.

    Args:
        user_id: The ID of the target user.
        badge_id: The ID of the badge to revoke.
    
    Raises:
        ValueError: if the given badge does not exist.
        ValueError: if the user has not been awarded with the given badge.
    """
    # Validate that the badge exists
    badge = badge_repository._repository.find_by_id(badge_id)
    if badge is None:
        raise ValueError("The given badge does not exist.")
    
    award_id = base._generate_award_id(user_id, badge_id)

    try:
        awarded_badge_repository._repository.delete(award_id)
    except KeyError:
        raise ValueError("The given user has not been awarded with this badge.")
    

def has_badge(
    user_id: uuid.UUID,
    badge_id: uuid.UUID,
    times: int = 1
) -> bool:
    """Checks whether a user has earned a given badge.

    Args:
        user_id: The ID of the target user.
        badge_id: The ID of the badge to check.
        times: An optional threshold to check against the number of times that
            the user has been awarded with the given badge. If not given,
            ``times`` defaults to a value of 1.
    
    Returns:
        Whether the user has been awarded with the badge at least ``times``
        times.

    Raises:
        ValueError: if the given Badge does not exist
        ValueError: if ``times < 1``
        ValueError: if ``times > 1``, despite the badge not being repeatable
    """
    # Validation
    badge = badge_repository._repository.find_by_id(badge_id)
    if badge is None:
        raise ValueError("The given badge does not exist.")
    
    if times < 1:
        raise ValueError("An Badge cannot be awarded <1 times: ``times`` must"
                         + " be at least 1.")
    elif times > 1 and not badge.repeatable:
        raise ValueError("Received a ``times`` parameter of >1, but the given"
                         + " badge is not repeatable.")

    award_id = base._generate_award_id(user_id, badge_id)
    award = awarded_badge_repository._repository.find_by_id(award_id)

    if award is None:
        return False

    return award.times_awarded >= times
    

def get_user_badges(user_id: uuid.UUID) -> list[base.Badge]:
    """Retrieves a list of IDs of the badges that the user has earned.

    Args:
        user_id: The ID of the target user.

    Returns:
        A list of the IDs of the user's earned badges; will never be None, but
        may be empty.

    Raises:
        ValueError: if the user has been awarded with a badge that does not
            exist.
    """
    
    # Retrieve all award records from the repository and initialise the user's
    # badge list.
    all_awards = awarded_badge_repository._repository.find_all()
    badge_list: list[base.Badge] = []

    for award in all_awards:
        # If the current award points to the target user, then add its badge to
        # the list
        if award.user_id == user_id:
            badge = badge_repository._repository.find_by_id(award.badge_id)

            if badge is None:
                raise ValueError("A badge which does not exist has been awarded"
                                 + " to the user")

            badge_list.append(badge)
    
    return badge_list


def evaluate_badges(context: predicates.AwardContext) -> list[base.Badge]:
    """Evaluates the given award context, and automatically awards any badges
    that the user is eligible to receive.
    
    If a badge is not yet held, it is simply awarded if the given context
    satisfies the badge's award condition.
    
    If the badge is already held, but is repeatable, then it is evaluated
    against the award context since its last award datetime.
    
    If a badge is already held and not repeatable, it is simply skipped. 
    
    Args:
        context: The snapshot of user activity against which to evaluate.
        
    Returns:
        A list of the badges awarded or re-awarded by this call.
    """
    badges = badge_repository._repository.find_all()
    badges_awarded: list[base.Badge] = []
    user_id = context.user_id

    for badge in badges:

        # Determine whether the user has already been awarded with the current
        # badge
        existing_award_id = base._generate_award_id(user_id, badge.get_id())
        existing_award = awarded_badge_repository._repository.find_by_id(
            existing_award_id
        )
        
        # If the user doesn't have the current badge, then check against the
        # entire context window provided
        if existing_award is None:
            if badge.award_condition.is_satisfied_by(context):
                award_badge(context.user_id, badge.get_id())
                badges_awarded.append(badge)
                continue
        
        # If the user does already have at least one of the current badge, and
        # it is repeatable, then only evaluate against the context window since
        # the most recent award
        elif badge.repeatable:
            recent_context = context.since(existing_award.awarded_at)
            if badge.award_condition.is_satisfied_by(recent_context):
                award_badge(context.user_id, badge.get_id())
                badges_awarded.append(badge)
                continue

        # If this point is reached, then the user must already have been awarded
        # with the given badge, however it is not repeatable, meaning no
        # evaluation is required.

    return badges_awarded
            