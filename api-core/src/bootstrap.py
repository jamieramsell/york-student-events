"""Composition root for the api-core service.

The single place where the service graph is assembled: ``bootstrap()`` 
constructs each slice's repositories, injects them into their services (and
those services into one another), and registers the ``activity`` subscriptions
that drive automatic badge evaluation. Concentrating this wiring here keeps it
out of the individual slices, meaning that importing a slice has no wiring side
effects, and gives every entry point one shared definition of how the graph fits
together, mirroring event-service's ``Application``.

Both entry points go through this module rather than wiring their own graph: the
bridge responder assembles a graph over the canned repositories per subprocess
call, and the test suite assembles a fresh graph over its own bare repositories
per test.

The public surface is ``bootstrap()`` (the wiring function) and the ``Services``
container it returns, which holds the constructed repositories and services.
"""

import attendance
import badges
import dataclasses
import friends
import recommendations


@dataclasses.dataclass(frozen=True, kw_only=True)
class Services:
    """Immutable container for the fully wired api-core service graph.

    Produced by ``bootstrap()``: every repository and service is constructed
    once, injected into its dependents, and gathered here, so that the entry
    points (the bridge responder and the test suite) can read a single object
    which has already been wired, instead of re-assembling the graph themselves.
    The contained is frozen so that the composed graph cannot be re-bound after
    construction.

    Attributes:
        attendance_repo: Backing store for ``Attendance`` records; injected into
            ``attendance_service``.
        friendship_repo: Backing store for ``Friendship`` records; injected into
            ``friendship_service``.
        badge_repo: Backing store for ``Badge`` definitions; injected into
            ``badge_service``.
        awarded_badge_repo: Backing store for ``AwardedBadge`` records (the
            badges that a user has earned); injected into ``badge_service``.
        attendance_service: Records and queries event attendance.
        friendship_service: Drives the friend graph by sending, accepting, and
            removing requests, and querying friendships.
        badge_service: Creates, awards, revokes, and condition-evaluates badges.
        recommendations_service: Generates event and friend recommendations by
            mining the friend graph; depends on ``friendship_service``.
        evaluation_service: Activity-driven listener that re-evaluates a user's
            badges whenever another slice publishes a change; depends on
            ``attendance_service``, ``friendship_service``, and
            ``badge_service``, and is subscribed to the ``activity`` registry by
            ``bootstrap()``.
    """
    attendance_repo: attendance.AttendanceRepository
    friendship_repo: friends.FriendshipRepository
    badge_repo: badges.BadgeRepository
    awarded_badge_repo: badges.AwardedBadgeRepository
    attendance_service: attendance.AttendanceService
    friendship_service: friends.FriendshipService
    badge_service: badges.BadgeService
    recommendations_service: recommendations.RecommendationsService
    evaluation_service: badges.EvaluationService


def bootstrap(
    *,
    attendance_repository: attendance.AttendanceRepository | None = None,
    friendship_repository: friends.FriendshipRepository | None = None,
    badge_repository: badges.BadgeRepository | None = None,
    awarded_badge_repository: badges.AwardedBadgeRepository | None = None,
) -> Services:
    """Composition root which constructs, wires, and registers the api-core
    service graph.

    Builds each service from its repository by constructor injection, and wires
    the services into one another, assembling the whole dependency graph in one
    place. Any repository not supplied defaults to a fresh, empty in-memory
    implementation. Callers that require a seeded state (e.g. the v0.4.0 bridge
    responder) pass the canned repositories, and tests pass their own instances
    so they can assert against them. Registers ``evaluation_service`` with the
    ``activity`` registry, so a change published by any slice automatically
    triggers badge re-evaluation.

    ``EvaluationService.register()`` mutates the process-wide ``activity``
    listener set, so calling ``bootstrap`` more than once in a single process
    subscribes an additional listener each time. A caller that re-bootstraps
    (such as the test suite) should clear that registry between calls.

    Args:
        attendance_repository: Store backing ``attendance_service``. Defaults to
            a fresh empty ``InMemoryAttendanceRepository``.
        friendship_repository: Store backing ``friendship_service``. Defaults to
            a fresh empty ``InMemoryFriendshipRepository``.
        badge_repository: Store of badge definitions backing ``badge_service``.
            Defaults to a fresh empty ``InMemoryBadgeRepository``.
        awarded_badge_repository: Store of earned badges backing
            ``badge_service``. Defaults to a fresh empty
            ``InMemoryAwardedBadgeRepository``.

    Returns:
        A ``Services`` container holding the repositories and the fully wired,
        ``activity``-registered services.
    """
    attendance_repository = (
        attendance_repository
        if attendance_repository is not None
        else attendance.InMemoryAttendanceRepository()
    )
    friendship_repository = (
        friendship_repository
        if friendship_repository is not None
        else friends.InMemoryFriendshipRepository()
    )
    badge_repository = (
        badge_repository
        if badge_repository is not None
        else badges.InMemoryBadgeRepository()
    )
    awarded_badge_repository = (
        awarded_badge_repository
        if awarded_badge_repository is not None
        else badges.InMemoryAwardedBadgeRepository()
    )

    attendance_service = attendance.AttendanceService(attendance_repository)
    friendship_service = friends.FriendshipService(friendship_repository)
    badge_service = badges.BadgeService(badge_repository,
                                        awarded_badge_repository)
    recommendations_service = recommendations.RecommendationsService(
        friendship_service
    )
    evaluation_service = badges.EvaluationService(
        attendance_service, friendship_service, badge_service
    )

    evaluation_service.register()

    services = Services(
        attendance_repo=attendance_repository,
        friendship_repo=friendship_repository,
        badge_repo=badge_repository,
        awarded_badge_repo=awarded_badge_repository,
        attendance_service=attendance_service,
        friendship_service=friendship_service,
        badge_service=badge_service,
        recommendations_service=recommendations_service,
        evaluation_service=evaluation_service
    )

    return services