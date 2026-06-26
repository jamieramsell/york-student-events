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

import uuid

import pytest

import bridge
import friends
from matching.matching import get_recommended_events


@pytest.fixture
def world(monkeypatch):
    """Configurable fake friend graph and event source.

    Returns a dict with two sub-dicts the test can fill in:

      * ``friends`` — maps a user id to the list of their friends' ids.
      * ``events``  — maps a user id to the list of event ids they are
            registered for.

    Any id not present defaults to an empty list.
    """

    state = {"friends": {}, "events": {}}

    def fake_get_friends(user_id):
        return state["friends"].get(user_id, [])

    def fake_get_user_events(user_id):
        return state["events"].get(user_id, [])

    monkeypatch.setattr(friends, "get_friends", fake_get_friends)
    monkeypatch.setattr(bridge, "get_user_events", fake_get_user_events)
    return state


class TestGetRecommendedEvents:
    """Behavioural tests for ``get_recommended_events``."""

    def test_no_friends_returns_empty(self, world):
        user = uuid.uuid4()
        world["friends"][user] = []

        assert get_recommended_events(user) == []

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

        assert get_recommended_events(user) == []

    def test_single_friend_single_event(self, world):
        user, friend = uuid.uuid4(), uuid.uuid4()
        event = uuid.uuid4()
        world["friends"][user] = [friend]
        world["events"][friend] = [event]

        assert get_recommended_events(user) == [event]

    def test_returns_all_events_a_friend_attends(self, world):
        user, friend = uuid.uuid4(), uuid.uuid4()
        e1, e2, e3 = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        world["friends"][user] = [friend]
        world["events"][friend] = [e1, e2, e3]

        assert set(get_recommended_events(user)) == {e1, e2, e3}

    def test_ranked_by_number_of_attending_friends(self, world):
        user = uuid.uuid4()
        a, b, c = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        popular, niche = uuid.uuid4(), uuid.uuid4()
        world["friends"][user] = [a, b, c]
        # `popular` is attended by all three friends, `niche` by only one.
        world["events"][a] = [popular, niche]
        world["events"][b] = [popular]
        world["events"][c] = [popular]

        result = get_recommended_events(user)

        assert result == [popular, niche]

    def test_full_descending_order(self, world):
        user = uuid.uuid4()
        a, b, c = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        top, mid, low = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        world["friends"][user] = [a, b, c]
        world["events"][a] = [top, mid, low]
        world["events"][b] = [top, mid]
        world["events"][c] = [top]

        assert get_recommended_events(user) == [top, mid, low]

    def test_excludes_events_the_user_already_registered_for(self, world):
        user, friend = uuid.uuid4(), uuid.uuid4()
        already, fresh = uuid.uuid4(), uuid.uuid4()
        world["friends"][user] = [friend]
        world["events"][user] = [already]
        world["events"][friend] = [already, fresh]

        assert get_recommended_events(user) == [fresh]

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
        assert get_recommended_events(user) == [fresh]

    def test_all_recommendations_excluded_returns_empty(self, world):
        user, friend = uuid.uuid4(), uuid.uuid4()
        e1, e2 = uuid.uuid4(), uuid.uuid4()
        world["friends"][user] = [friend]
        world["events"][user] = [e1, e2]
        world["events"][friend] = [e1, e2]

        assert get_recommended_events(user) == []

    def test_friend_with_no_events_contributes_nothing(self, world):
        user = uuid.uuid4()
        active, inactive = uuid.uuid4(), uuid.uuid4()
        event = uuid.uuid4()
        world["friends"][user] = [active, inactive]
        world["events"][active] = [event]
        world["events"][inactive] = []

        assert get_recommended_events(user) == [event]

    def test_friends_with_no_events_at_all_returns_empty(self, world):
        user = uuid.uuid4()
        a, b = uuid.uuid4(), uuid.uuid4()
        world["friends"][user] = [a, b]
        # neither friend (nor the user) is registered for anything

        assert get_recommended_events(user) == []

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
        assert get_recommended_events(user) == [shared, single]

    def test_result_contains_no_duplicates(self, world):
        user = uuid.uuid4()
        a, b = uuid.uuid4(), uuid.uuid4()
        event = uuid.uuid4()
        world["friends"][user] = [a, b]
        world["events"][a] = [event]
        world["events"][b] = [event]

        result = get_recommended_events(user)

        assert result == [event]
        assert len(result) == len(set(result))

    def test_returns_a_list(self, world):
        user, friend = uuid.uuid4(), uuid.uuid4()
        world["friends"][user] = [friend]
        world["events"][friend] = [uuid.uuid4()]

        assert isinstance(get_recommended_events(user), list)
