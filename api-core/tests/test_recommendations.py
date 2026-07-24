"""Unit tests for the matching slice (``matching.get_recommended_events``).

``get_recommended_events`` orchestrates two collaborators:

  * ``friends.get_friends``      — the user's accepted friends.
  * ``bridge.get_user_events``   — the events a given user is registered for,
        fetched from event-service over the subprocess bridge.

Both are stubbed here so the recommendation logic can be exercised in isolation,
deterministically and without spawning a JVM. The ``world`` fixture installs
fakes backed by plain dicts that each test populates.

Run from the repo root:  ``python -m pytest api-core/tests/``
"""

import sys
import uuid

import bridge
import pytest
from friends import FriendshipService, InMemoryFriendshipRepository
from recommendations import RecommendationsService

# Module-level defaults so the file is usable on its own; ``compose_services`` in
# conftest.py swaps in a fresh ``friendship_service`` and a
# ``recommendations_service`` wrapping it before every test.
friendship_service = FriendshipService(InMemoryFriendshipRepository())
recommendations_service = RecommendationsService(friendship_service)


@pytest.fixture
def world(monkeypatch):
    """Configurable fake friend graph and event source.

    Returns a dict with two sub-dicts the test can fill in:

      * ``friends`` — maps a user id to the list of their friends' ids.
      * ``events``  — maps a user id to the list of event ids they are
            registered for.

    Any id not present defaults to an empty list. Rather than reach into the
    friend repository, the fake graph is injected as a stand-in
    ``FriendshipService`` behind a ``RecommendationsService``, so the
    recommendation logic is exercised in isolation.
    """

    state = {"friends": {}, "events": {}}

    class _FakeFriendGraph:
        def get_friends(self, user_id):
            return state["friends"].get(user_id, [])

    def fake_get_user_events(user_id):
        return state["events"].get(user_id, [])

    monkeypatch.setattr(
        sys.modules[__name__],
        "recommendations_service",
        RecommendationsService(_FakeFriendGraph()),
    )
    monkeypatch.setattr(bridge, "get_user_events", fake_get_user_events)
    return state


class TestGetRecommendedEvents:
    """Behavioural tests for ``get_recommended_events``."""

    def test_no_friends_returns_empty(self, world):
        user = uuid.uuid4()
        world["friends"][user] = []

        assert recommendations_service.get_recommended_events(user) == []

    def test_no_friends_does_not_query_the_event_bridge(self, monkeypatch, world):
        """With no friends there is nothing to recommend, so the bridge (and the
        JVM behind it) should never be invoked."""

        user = uuid.uuid4()
        world["friends"][user] = []

        def boom(_user_id):
            raise AssertionError(
                "bridge.get_user_events must not be called when the user "
                "has no friends"
            )

        monkeypatch.setattr(bridge, "get_user_events", boom)

        assert recommendations_service.get_recommended_events(user) == []

    def test_single_friend_single_event(self, world):
        user, friend = uuid.uuid4(), uuid.uuid4()
        event = uuid.uuid4()
        world["friends"][user] = [friend]
        world["events"][friend] = [event]

        assert recommendations_service.get_recommended_events(user) == [event]

    def test_returns_all_events_a_friend_attends(self, world):
        user, friend = uuid.uuid4(), uuid.uuid4()
        e1, e2, e3 = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        world["friends"][user] = [friend]
        world["events"][friend] = [e1, e2, e3]

        assert set(recommendations_service.get_recommended_events(user)) == {e1, e2, e3}

    def test_ranked_by_number_of_attending_friends(self, world):
        user = uuid.uuid4()
        a, b, c = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        popular, niche = uuid.uuid4(), uuid.uuid4()
        world["friends"][user] = [a, b, c]
        # `popular` is attended by all three friends, `niche` by only one.
        world["events"][a] = [popular, niche]
        world["events"][b] = [popular]
        world["events"][c] = [popular]

        result = recommendations_service.get_recommended_events(user)

        assert result == [popular, niche]

    def test_full_descending_order(self, world):
        user = uuid.uuid4()
        a, b, c = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        top, mid, low = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        world["friends"][user] = [a, b, c]
        world["events"][a] = [top, mid, low]
        world["events"][b] = [top, mid]
        world["events"][c] = [top]

        assert recommendations_service.get_recommended_events(user) == [top, mid, low]

    def test_excludes_events_the_user_already_registered_for(self, world):
        user, friend = uuid.uuid4(), uuid.uuid4()
        already, fresh = uuid.uuid4(), uuid.uuid4()
        world["friends"][user] = [friend]
        world["events"][user] = [already]
        world["events"][friend] = [already, fresh]

        assert recommendations_service.get_recommended_events(user) == [fresh]

    def test_excludes_already_registered_even_when_many_friends_attend(self, world):
        user = uuid.uuid4()
        a, b, c = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        already, fresh = uuid.uuid4(), uuid.uuid4()
        world["friends"][user] = [a, b, c]
        world["events"][user] = [already]
        world["events"][a] = [already]
        world["events"][b] = [already]
        world["events"][c] = [fresh]

        # `already` outranks `fresh` by friend count but must still be dropped.
        assert recommendations_service.get_recommended_events(user) == [fresh]

    def test_all_recommendations_excluded_returns_empty(self, world):
        user, friend = uuid.uuid4(), uuid.uuid4()
        e1, e2 = uuid.uuid4(), uuid.uuid4()
        world["friends"][user] = [friend]
        world["events"][user] = [e1, e2]
        world["events"][friend] = [e1, e2]

        assert recommendations_service.get_recommended_events(user) == []

    def test_friend_with_no_events_contributes_nothing(self, world):
        user = uuid.uuid4()
        active, inactive = uuid.uuid4(), uuid.uuid4()
        event = uuid.uuid4()
        world["friends"][user] = [active, inactive]
        world["events"][active] = [event]
        world["events"][inactive] = []

        assert recommendations_service.get_recommended_events(user) == [event]

    def test_friends_with_no_events_at_all_returns_empty(self, world):
        user = uuid.uuid4()
        a, b = uuid.uuid4(), uuid.uuid4()
        world["friends"][user] = [a, b]
        # neither friend (nor the user) is registered for anything

        assert recommendations_service.get_recommended_events(user) == []

    def test_duplicate_event_for_one_friend_counts_once(self, world):
        """A single friend listing the same event twice must not inflate its
        rank: the per-friend list is de-duplicated before counting."""

        user = uuid.uuid4()
        a, b, c = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        single, shared = uuid.uuid4(), uuid.uuid4()
        world["friends"][user] = [a, b, c]
        world["events"][a] = [single, single]  # duplicate within one friend
        world["events"][b] = [shared]
        world["events"][c] = [shared]

        # If dedup works: shared=2, single=1 -> [shared, single].
        # Without dedup: single=2, shared=2 -> ambiguous tie.
        assert recommendations_service.get_recommended_events(user) == [shared, single]

    def test_result_contains_no_duplicates(self, world):
        user = uuid.uuid4()
        a, b = uuid.uuid4(), uuid.uuid4()
        event = uuid.uuid4()
        world["friends"][user] = [a, b]
        world["events"][a] = [event]
        world["events"][b] = [event]

        result = recommendations_service.get_recommended_events(user)

        assert result == [event]
        assert len(result) == len(set(result))

    def test_returns_a_list(self, world):
        user, friend = uuid.uuid4(), uuid.uuid4()
        world["friends"][user] = [friend]
        world["events"][friend] = [uuid.uuid4()]

        assert isinstance(recommendations_service.get_recommended_events(user), list)


class TestFindNewFriends:
    """Behavioural tests for ``find_new_friends``.

    Unlike ``get_recommended_events``, ``find_new_friends`` mines the friend
    *graph* rather than the event bridge, so these tests drive the real
    ``FriendshipService`` (``send_friend_request`` / ``accept_friend_request``)
    that the ``RecommendationsService`` wraps -- the ``compose_services`` fixture
    in ``conftest.py`` injects a clean, shared graph per test. No bridge or JVM
    is involved.
    """

    @staticmethod
    def _befriend(a, b):
        """Sends and immediately accepts a request, establishing a friendship."""

        friendship_service.send_friend_request(a, b)
        friendship_service.accept_friend_request(a, b)

    def test_no_friends_returns_empty_set(self):
        # A user absent from the graph entirely has nothing to recommend, and
        # must not surface the "user not found" error from get_friend_circle.
        assert recommendations_service.find_new_friends(uuid.uuid4()) == set()

    def test_friend_with_no_friends_of_friends_returns_empty_set(self):
        a, b = uuid.uuid4(), uuid.uuid4()
        self._befriend(a, b)
        assert recommendations_service.find_new_friends(a) == set()

    def test_recommends_a_friend_of_a_friend(self):
        a, b, c = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        self._befriend(a, b)
        self._befriend(b, c)  # c shares friend b with a
        assert recommendations_service.find_new_friends(a) == {c}

    def test_does_not_recommend_existing_direct_friends(self):
        # A fully connected triangle: every friend-of-friend is already a direct
        # friend, so there is nobody new to recommend.
        a, b, c = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        self._befriend(a, b)
        self._befriend(a, c)
        self._befriend(b, c)
        assert recommendations_service.find_new_friends(a) == set()

    def test_excludes_users_with_a_pending_request(self):
        # c is a friend-of-friend via b, but a already has a pending request
        # with c, so c is not a "new" friend to recommend.
        a, b, c = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        self._befriend(a, b)
        self._befriend(b, c)
        friendship_service.send_friend_request(a, c)  # pending, never accepted
        assert recommendations_service.find_new_friends(a) == set()

    def test_delves_deeper_when_target_not_met(self):
        # With only a handful of candidates (far below the internal target),
        # the search descends past the immediate friends-of-friends layer.
        a, b, c, d = (uuid.uuid4() for _ in range(4))
        self._befriend(a, b)
        self._befriend(b, c)
        self._befriend(c, d)
        assert recommendations_service.find_new_friends(a) == {c, d}

    def test_recommends_from_multiple_mutual_friends(self):
        # a has two friends, b and d, each of whom introduces a new candidate.
        a, b, c, d, e = (uuid.uuid4() for _ in range(5))
        self._befriend(a, b)
        self._befriend(a, d)
        self._befriend(b, c)
        self._befriend(d, e)
        assert recommendations_service.find_new_friends(a) == {c, e}

    def test_returns_a_set(self):
        a, b, c = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        self._befriend(a, b)
        self._befriend(b, c)
        assert isinstance(recommendations_service.find_new_friends(a), set)
