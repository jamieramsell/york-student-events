"""Integration tests for the matching slice (``matching.get_recommended_events``).

Where ``test_matching.py`` stubs both of ``get_recommended_events``'s
collaborators to exercise the recommendation logic in isolation, this module
exercises the *integration seam*: the same entry point driven against the real
collaborators it wires together.

  * The friend graph is built through the **real** ``FriendshipService``
    (``send_friend_request`` / ``accept_friend_request``) backed by a fresh
    ``InMemoryFriendshipRepository``. The ``compose_services`` fixture in
    ``conftest.py`` injects a clean service graph per test, so no state leaks
    between tests.
  * Event lookups are driven through the **real** subprocess bridge
    (``bridge.get_user_events``), which spawns the Java
    ``york.studentevents.subprocess.SubprocessResponder`` as a child process and
    exchanges JSON over stdio.

None of the unit-test fakes are reused here -- that would defeat the purpose of
testing the seam.

JVM-backed tests
----------------
The bridge tests require a compiled ``event-service`` classpath. Run
``./mvnw compile`` in ``event-service/`` first (this produces ``target/classes``
and ``target/cp.txt``). When those build artifacts are absent -- e.g. on a clean
checkout or in a Python-only CI job -- the JVM-backed tests skip gracefully via
``requires_event_service`` rather than fail, so the suite stays green. They are
also tagged ``@pytest.mark.integration`` so they can be selected or deselected
explicitly (``pytest -m integration`` / ``pytest -m 'not integration'``).

Empty-graph limitation
----------------------
Each request spawns the responder under ``YSE_BRIDGE_INMEMORY``, so it composes a
throwaway in-memory graph that starts *empty* and cannot see data seeded in this
process (the JVM is a separate process). ``get_user_events`` therefore raises for
every user here, so a *successful* multi-user recommendation cannot be driven
end-to-end against this bridge -- that arithmetic (friend-event exclusion and
ranking) is covered by the unit tests in ``test_matching.py``. The tests below
instead prove that ``get_recommended_events`` really delegates its event lookups
to the live responder: reaching the bridge surfaces the responder's own error
envelope as a ``SubprocessError``.

Run from the repo root:  ``python -m pytest api-core/tests/``
"""

import uuid

import pytest

import bridge
from bridge import client as bridge_client
from friends import FriendshipService, InMemoryFriendshipRepository
from recommendations import RecommendationsService

# Module-level defaults so the file is usable on its own; ``compose_services`` in
# conftest.py swaps in a fresh ``friendship_service`` and a
# ``recommendations_service`` wrapping it before every test.
friendship_service = FriendshipService(InMemoryFriendshipRepository())
recommendations_service = RecommendationsService(friendship_service)

@pytest.fixture(autouse=True)
def _inmemory_bridge(monkeypatch):
    monkeypatch.setenv("YSE_BRIDGE_INMEMORY", "1")

    
def _event_service_built() -> bool:
    """Whether event-service has been compiled (``./mvnw compile``).

    Reuses the bridge client's own notion of where the build output lives, so
    this guard stays in step with what the client actually requires at call
    time: ``target/classes`` and ``target/cp.txt``.
    """

    return (
        bridge_client._CLASSES_DIR.is_dir()
        and bridge_client._CLASSPATH_FILE.is_file()
    )


# Skip JVM-backed tests cleanly when event-service has not been built, instead
# of failing on a missing classpath.
requires_event_service = pytest.mark.skipif(
    not _event_service_built(),
    reason=(
        "event-service is not built: target/classes and/or target/cp.txt are "
        "absent. Run `./mvnw compile` in event-service/ to enable the "
        "JVM-backed integration tests."
    ),
)


class TestMatchingFriendsSeam:
    """``get_recommended_events`` against the real friends service.

    These cases stay on the friends side of the seam: the friend graph is built
    through the real service API and the real in-memory repository. They need no
    JVM because the scenarios short-circuit before the bridge is consulted -- a
    user with no accepted friends has nothing to recommend.
    """

    def test_no_friends_short_circuits(self):
        """With an empty friend graph, recommendations are empty and the bridge
        is never reached (so the JVM is not required)."""

        user = uuid.uuid4()

        # No friendships have been created, so the real get_friends() returns []
        # and get_recommended_events short-circuits before touching the bridge.
        assert friendship_service.get_friends(user) == []
        assert recommendations_service.get_recommended_events(user) == []

    def test_pending_friendship_does_not_contribute(self):
        """A sent-but-not-accepted request is not a friendship: it contributes
        no friends, so there is nothing to recommend."""

        user, invitee = uuid.uuid4(), uuid.uuid4()

        # Request sent but deliberately not accepted -> still PENDING.
        friendship_service.send_friend_request(user, invitee)

        assert friendship_service.get_friends(user) == []
        assert recommendations_service.get_recommended_events(user) == []

    def test_accepted_request_establishes_a_friend(self):
        """Sanity check on the friends seam itself: an accepted request makes
        the two users friends of each other through the real service."""

        user, friend = uuid.uuid4(), uuid.uuid4()

        friendship_service.send_friend_request(user, friend)
        friendship_service.accept_friend_request(user, friend)

        assert friendship_service.get_friends(user) == [friend]
        assert friendship_service.get_friends(friend) == [user]


@pytest.mark.integration
@requires_event_service
class TestRealBridge:
    """``get_recommended_events`` and the bridge against the live responder.

    Every test here spawns the real Java ``SubprocessResponder`` over an empty
    in-memory graph (see the module docstring), so no user resolves: these cases
    prove the bridge is actually reached, surfacing the responder's error
    envelope as a ``SubprocessError``.
    """

    def test_bridge_raises_not_recognised_for_an_unknown_user(self):
        """A full round-trip through the real responder: spawn the JVM, send a
        GET_USER_EVENTS request, and confirm the empty graph rejects the user.

        Matching the responder's own ``not recognised`` message proves the JVM
        ran and returned an error envelope, rather than the client failing to
        launch it.
        """

        with pytest.raises(bridge.SubprocessError, match="not recognised"):
            bridge.get_user_events(uuid.uuid4())

    def test_recommendation_delegates_event_lookups_to_the_responder(self):
        """``get_recommended_events`` really fetches event data over the bridge.

        With one real accepted friend the pipeline gets past the no-friends
        short-circuit and makes its first bridge call: a ``get_user_events``
        lookup for the target's own events. The empty graph does not recognise
        that user, so the responder's error envelope propagates as a
        ``SubprocessError`` -- proving the lookup was delegated to the live
        responder rather than served locally.

        A successful multi-user recommendation cannot be asserted until the
        bridge serves real event data for seeded users; the exclusion and
        ranking arithmetic itself is covered by the unit tests in
        ``test_matching.py``.
        """

        user, friend = uuid.uuid4(), uuid.uuid4()
        friendship_service.send_friend_request(user, friend)
        friendship_service.accept_friend_request(user, friend)
        assert friendship_service.get_friends(user) == [friend]

        with pytest.raises(bridge.SubprocessError, match="not recognised"):
            recommendations_service.get_recommended_events(user)
