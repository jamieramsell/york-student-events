"""Comprehensive unit and integration tests for the friends slice.

Layout mirrors the package's three modules:

  * ``TestGenerateId`` / ``TestFriendshipStatus`` / ``TestFriendship``
        unit tests for ``base`` (the domain model).
  * ``TestInMemoryFriendshipRepository``
        unit tests for ``friendship_repository`` (storage, in isolation).
  * ``TestFriendshipService``
        integration tests for ``friendship_service``, exercising the service
        functions through the real in-memory repository singleton.

Run from the repo root:  ``python -m pytest api-core/tests/``
"""

import dataclasses
import datetime
import uuid

import pytest

import friends.friendship_repository as friendship_repository
import friends.friendship_service as friendship_service
import repositories
from friends.base import Friendship, FriendshipStatus, _generate_id
from friends.friendship_repository import InMemoryFriendshipRepository

_FIXED_TIME = datetime.datetime(2026, 6, 20, 12, 0, 0)


def _friendship(
    user_id=None,
    friend_id=None,
    status=FriendshipStatus.PENDING,
    created_at=_FIXED_TIME,
):
    """Builds a Friendship with sensible, overridable defaults."""

    return Friendship(
        user_id if user_id is not None else uuid.uuid4(),
        friend_id if friend_id is not None else uuid.uuid4(),
        created_at,
        status,
    )


def _repo():
    """Returns the live service repository singleton."""

    return friendship_repository._repository


# ---------------------------------------------------------------------------
# base._generate_id
# ---------------------------------------------------------------------------
class TestGenerateId:
    def test_returns_frozenset_of_both_ids(self):
        a, b = uuid.uuid4(), uuid.uuid4()
        result = _generate_id(a, b)
        assert isinstance(result, frozenset)
        assert result == frozenset({a, b})

    def test_is_symmetric(self):
        a, b = uuid.uuid4(), uuid.uuid4()
        assert _generate_id(a, b) == _generate_id(b, a)

    def test_distinct_ids_yield_two_element_key(self):
        assert len(_generate_id(uuid.uuid4(), uuid.uuid4())) == 2

    def test_equal_ids_raise_value_error(self):
        same = uuid.uuid4()
        with pytest.raises(ValueError):
            _generate_id(same, same)


# ---------------------------------------------------------------------------
# base.FriendshipStatus
# ---------------------------------------------------------------------------
class TestFriendshipStatus:
    def test_has_exactly_two_members(self):
        assert set(FriendshipStatus) == {
            FriendshipStatus.PENDING,
            FriendshipStatus.ACCEPTED,
        }

    def test_member_values(self):
        assert FriendshipStatus.PENDING.value == "pending"
        assert FriendshipStatus.ACCEPTED.value == "accepted"


# ---------------------------------------------------------------------------
# base.Friendship
# ---------------------------------------------------------------------------
class TestFriendship:
    def test_stores_attributes(self):
        a, b = uuid.uuid4(), uuid.uuid4()
        f = Friendship(a, b, _FIXED_TIME, FriendshipStatus.PENDING)
        assert f.user_id == a
        assert f.friend_id == b
        assert f.created_at == _FIXED_TIME
        assert f.friendship_status == FriendshipStatus.PENDING

    def test_is_an_ientity(self):
        assert isinstance(_friendship(), repositories.IEntity)

    def test_get_id_matches_generate_id(self):
        a, b = uuid.uuid4(), uuid.uuid4()
        f = Friendship(a, b, _FIXED_TIME, FriendshipStatus.PENDING)
        assert f.get_id() == _generate_id(a, b)

    def test_get_id_is_direction_independent(self):
        a, b = uuid.uuid4(), uuid.uuid4()
        forward = Friendship(a, b, _FIXED_TIME, FriendshipStatus.PENDING)
        reverse = Friendship(b, a, _FIXED_TIME, FriendshipStatus.PENDING)
        assert forward.get_id() == reverse.get_id()

    def test_is_frozen(self):
        f = _friendship()
        with pytest.raises(dataclasses.FrozenInstanceError):
            f.friendship_status = FriendshipStatus.ACCEPTED

    def test_equality_by_value(self):
        a, b = uuid.uuid4(), uuid.uuid4()
        f1 = Friendship(a, b, _FIXED_TIME, FriendshipStatus.PENDING)
        f2 = Friendship(a, b, _FIXED_TIME, FriendshipStatus.PENDING)
        assert f1 == f2

    def test_is_hashable(self):
        f = _friendship()
        # Frozen dataclasses are hashable; this must not raise.
        assert f in {f}


# ---------------------------------------------------------------------------
# friendship_repository.InMemoryFriendshipRepository
# ---------------------------------------------------------------------------
class TestInMemoryFriendshipRepository:
    def test_new_repository_is_empty(self):
        assert InMemoryFriendshipRepository().find_all() == []

    def test_save_then_find_by_id_returns_entity(self):
        repo = InMemoryFriendshipRepository()
        f = _friendship()
        repo.save(f)
        assert repo.find_by_id(f.get_id()) is f

    def test_find_by_id_missing_returns_none(self):
        repo = InMemoryFriendshipRepository()
        assert repo.find_by_id(_generate_id(uuid.uuid4(), uuid.uuid4())) is None

    def test_find_all_returns_a_list(self):
        repo = InMemoryFriendshipRepository()
        repo.save(_friendship())
        assert isinstance(repo.find_all(), list)

    def test_find_all_contains_all_saved_entities(self):
        repo = InMemoryFriendshipRepository()
        f1 = _friendship()
        f2 = _friendship()
        repo.save(f1)
        repo.save(f2)
        assert set(repo.find_all()) == {f1, f2}

    def test_save_overwrites_same_key(self):
        repo = InMemoryFriendshipRepository()
        a, b = uuid.uuid4(), uuid.uuid4()
        repo.save(Friendship(a, b, _FIXED_TIME, FriendshipStatus.PENDING))
        repo.save(Friendship(a, b, _FIXED_TIME, FriendshipStatus.ACCEPTED))
        stored = repo.find_by_id(_generate_id(a, b))
        assert stored.friendship_status == FriendshipStatus.ACCEPTED
        assert len(repo.find_all()) == 1

    def test_reverse_direction_shares_key(self):
        repo = InMemoryFriendshipRepository()
        a, b = uuid.uuid4(), uuid.uuid4()
        repo.save(Friendship(a, b, _FIXED_TIME, FriendshipStatus.PENDING))
        repo.save(Friendship(b, a, _FIXED_TIME, FriendshipStatus.PENDING))
        # Same frozenset key, so the second save overwrites the first.
        assert len(repo.find_all()) == 1

    def test_delete_removes_entity(self):
        repo = InMemoryFriendshipRepository()
        f = _friendship()
        repo.save(f)
        repo.delete(f.get_id())
        assert repo.find_by_id(f.get_id()) is None

    def test_delete_missing_raises_key_error(self):
        repo = InMemoryFriendshipRepository()
        with pytest.raises(KeyError):
            repo.delete(_generate_id(uuid.uuid4(), uuid.uuid4()))


# ---------------------------------------------------------------------------
# friendship_service  (integration through the repository singleton)
# ---------------------------------------------------------------------------
class TestFriendshipService:
    # -- send_friend_request --------------------------------------------------
    def test_send_creates_pending_friendship(self):
        a, b = uuid.uuid4(), uuid.uuid4()
        friendship_service.send_friend_request(a, b)
        stored = _repo().find_by_id(_generate_id(a, b))
        assert stored is not None
        assert stored.friendship_status == FriendshipStatus.PENDING

    def test_send_records_sender_and_receiver(self):
        a, b = uuid.uuid4(), uuid.uuid4()
        friendship_service.send_friend_request(a, b)
        stored = _repo().find_by_id(_generate_id(a, b))
        assert stored.user_id == a
        assert stored.friend_id == b

    def test_send_sets_created_at_datetime(self):
        a, b = uuid.uuid4(), uuid.uuid4()
        friendship_service.send_friend_request(a, b)
        stored = _repo().find_by_id(_generate_id(a, b))
        assert isinstance(stored.created_at, datetime.datetime)

    def test_send_duplicate_pending_raises_value_error(self):
        a, b = uuid.uuid4(), uuid.uuid4()
        friendship_service.send_friend_request(a, b)
        with pytest.raises(ValueError):
            friendship_service.send_friend_request(a, b)

    def test_send_duplicate_is_direction_independent(self):
        a, b = uuid.uuid4(), uuid.uuid4()
        friendship_service.send_friend_request(a, b)
        with pytest.raises(ValueError):
            friendship_service.send_friend_request(b, a)

    def test_send_duplicate_after_accept_raises_value_error(self):
        a, b = uuid.uuid4(), uuid.uuid4()
        friendship_service.send_friend_request(a, b)
        friendship_service.accept_friend_request(a, b)
        with pytest.raises(ValueError):
            friendship_service.send_friend_request(a, b)

    def test_send_duplicate_does_not_overwrite_existing(self):
        a, b = uuid.uuid4(), uuid.uuid4()
        friendship_service.send_friend_request(a, b)
        original = _repo().find_by_id(_generate_id(a, b))
        with pytest.raises(ValueError):
            friendship_service.send_friend_request(a, b)
        assert _repo().find_by_id(_generate_id(a, b)) == original

    # -- accept_friend_request ------------------------------------------------
    def test_accept_sets_status_to_accepted(self):
        a, b = uuid.uuid4(), uuid.uuid4()
        friendship_service.send_friend_request(a, b)
        friendship_service.accept_friend_request(a, b)
        stored = _repo().find_by_id(_generate_id(a, b))
        assert stored.friendship_status == FriendshipStatus.ACCEPTED

    def test_accept_is_direction_independent(self):
        a, b = uuid.uuid4(), uuid.uuid4()
        friendship_service.send_friend_request(a, b)
        # Accept with arguments reversed relative to the original request.
        friendship_service.accept_friend_request(b, a)
        stored = _repo().find_by_id(_generate_id(a, b))
        assert stored.friendship_status == FriendshipStatus.ACCEPTED

    def test_accept_preserves_identity_and_created_at(self):
        a, b = uuid.uuid4(), uuid.uuid4()
        friendship_service.send_friend_request(a, b)
        before = _repo().find_by_id(_generate_id(a, b))
        friendship_service.accept_friend_request(a, b)
        after = _repo().find_by_id(_generate_id(a, b))
        assert after.user_id == before.user_id
        assert after.friend_id == before.friend_id
        assert after.created_at == before.created_at

    def test_accept_already_accepted_raises_value_error(self):
        a, b = uuid.uuid4(), uuid.uuid4()
        friendship_service.send_friend_request(a, b)
        friendship_service.accept_friend_request(a, b)
        with pytest.raises(ValueError):
            friendship_service.accept_friend_request(a, b)

    def test_accept_missing_raises_value_error(self):
        a, b = uuid.uuid4(), uuid.uuid4()
        with pytest.raises(ValueError):
            friendship_service.accept_friend_request(a, b)

    # -- removeFriend -------------------------------------------------------
    def test_remove_deletes_friendship(self):
        a, b = uuid.uuid4(), uuid.uuid4()
        friendship_service.send_friend_request(a, b)
        friendship_service.remove_friend(a, b)
        assert _repo().find_by_id(_generate_id(a, b)) is None

    def test_remove_is_direction_independent(self):
        a, b = uuid.uuid4(), uuid.uuid4()
        friendship_service.send_friend_request(a, b)
        friendship_service.remove_friend(b, a)
        assert _repo().find_by_id(_generate_id(a, b)) is None

    def test_remove_missing_raises_value_error(self):
        a, b = uuid.uuid4(), uuid.uuid4()
        with pytest.raises(ValueError):
            friendship_service.remove_friend(a, b)

    # -- get_friends ---------------------------------------------------------
    def test_get_friends_empty_when_none(self):
        assert friendship_service.get_friends(uuid.uuid4()) == []

    def test_get_friends_returns_accepted_friend_both_directions(self):
        a, b = uuid.uuid4(), uuid.uuid4()
        friendship_service.send_friend_request(a, b)
        friendship_service.accept_friend_request(a, b)
        assert friendship_service.get_friends(a) == [b]
        assert friendship_service.get_friends(b) == [a]

    def test_get_friends_excludes_pending(self):
        a, b = uuid.uuid4(), uuid.uuid4()
        friendship_service.send_friend_request(a, b)  # left PENDING
        assert friendship_service.get_friends(a) == []
        assert friendship_service.get_friends(b) == []

    def test_get_friends_returns_only_accepted_in_mixed_set(self):
        a, accepted, pending = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        friendship_service.send_friend_request(a, accepted)
        friendship_service.accept_friend_request(a, accepted)
        friendship_service.send_friend_request(a, pending)  # stays PENDING
        assert friendship_service.get_friends(a) == [accepted]

    def test_get_friends_returns_all_accepted_friends(self):
        a, b, c = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        friendship_service.send_friend_request(a, b)
        friendship_service.accept_friend_request(a, b)
        # c sends to a (a is the receiver this time); still returned for a.
        friendship_service.send_friend_request(c, a)
        friendship_service.accept_friend_request(c, a)
        assert set(friendship_service.get_friends(a)) == {b, c}

    # -- is_friend -----------------------------------------------------------
    def test_is_friend_false_when_no_friendship(self):
        assert friendship_service.is_friend(uuid.uuid4(), uuid.uuid4()) is False

    def test_is_friend_true_when_accepted(self):
        a, b = uuid.uuid4(), uuid.uuid4()
        friendship_service.send_friend_request(a, b)
        friendship_service.accept_friend_request(a, b)
        assert friendship_service.is_friend(a, b) is True

    def test_is_friend_false_when_pending(self):
        a, b = uuid.uuid4(), uuid.uuid4()
        friendship_service.send_friend_request(a, b)  # left PENDING
        assert friendship_service.is_friend(a, b) is False

    def test_is_friend_is_direction_independent(self):
        a, b = uuid.uuid4(), uuid.uuid4()
        friendship_service.send_friend_request(a, b)
        friendship_service.accept_friend_request(a, b)
        assert friendship_service.is_friend(b, a) is True

    def test_is_friend_false_after_removal(self):
        a, b = uuid.uuid4(), uuid.uuid4()
        friendship_service.send_friend_request(a, b)
        friendship_service.accept_friend_request(a, b)
        friendship_service.remove_friend(a, b)
        assert friendship_service.is_friend(a, b) is False

    def test_is_friend_same_user_raises_value_error(self):
        same = uuid.uuid4()
        with pytest.raises(ValueError):
            friendship_service.is_friend(same, same)

    # -- get_friend_circle ---------------------------------------------------
    @staticmethod
    def _befriend(a, b):
        """Sends and immediately accepts a request, yielding an accepted
        friendship between ``a`` and ``b``."""

        friendship_service.send_friend_request(a, b)
        friendship_service.accept_friend_request(a, b)

    def test_circle_unknown_user_raises_value_error(self):
        # A user absent from every friendship record cannot be found.
        with pytest.raises(ValueError):
            friendship_service.get_friend_circle(uuid.uuid4())

    def test_circle_negative_max_layers_raises_value_error(self):
        a, b = uuid.uuid4(), uuid.uuid4()
        self._befriend(a, b)
        with pytest.raises(ValueError):
            friendship_service.get_friend_circle(a, max_layers=-1)

    def test_circle_known_user_with_no_accepted_friends_is_empty(self):
        # A pending-only user is "known" (appears in a record) but reaches
        # no-one, so the circle is empty rather than an error.
        a, b = uuid.uuid4(), uuid.uuid4()
        friendship_service.send_friend_request(a, b)  # left PENDING
        assert friendship_service.get_friend_circle(a) == {}

    def test_circle_layer_zero_holds_direct_friends(self):
        a, b, c = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        self._befriend(a, b)
        self._befriend(a, c)
        circle = friendship_service.get_friend_circle(a)
        assert set(circle) == {0}
        assert set(circle[0]) == {b, c}

    def test_circle_excludes_the_target_user(self):
        a, b = uuid.uuid4(), uuid.uuid4()
        self._befriend(a, b)
        circle = friendship_service.get_friend_circle(a)
        assert all(a not in members for members in circle.values())

    def test_circle_layer_one_holds_friends_of_friends(self):
        a, b, c = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        self._befriend(a, b)
        self._befriend(b, c)  # c is a's friend-of-friend, not a direct friend
        circle = friendship_service.get_friend_circle(a)
        assert circle[0] == [b]
        assert circle[1] == [c]

    def test_circle_places_each_user_in_nearest_layer_only(self):
        # c is both a direct friend of a and a friend of b; it must appear only
        # in layer 0, never again in layer 1.
        a, b, c = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        self._befriend(a, b)
        self._befriend(a, c)
        self._befriend(b, c)
        circle = friendship_service.get_friend_circle(a)
        assert set(circle[0]) == {b, c}
        assert circle.get(1, []) == []

    def test_circle_excludes_pending_friends_of_friends(self):
        # b -> c is only pending, so c is never reached from a.
        a, b, c = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        self._befriend(a, b)
        friendship_service.send_friend_request(b, c)  # stays PENDING
        circle = friendship_service.get_friend_circle(a)
        assert circle == {0: [b]}

    def test_circle_max_layers_zero_yields_only_layer_zero(self):
        a, b, c = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        self._befriend(a, b)
        self._befriend(b, c)
        circle = friendship_service.get_friend_circle(a, max_layers=0)
        assert circle == {0: [b]}

    def test_circle_max_layers_one_yields_layers_zero_and_one(self):
        a, b, c, d = (uuid.uuid4() for _ in range(4))
        self._befriend(a, b)
        self._befriend(b, c)
        self._befriend(c, d)  # d sits at layer 2 and must be excluded
        circle = friendship_service.get_friend_circle(a, max_layers=1)
        assert set(circle) == {0, 1}
        assert circle[0] == [b]
        assert circle[1] == [c]

    def test_circle_none_max_layers_traverses_whole_chain(self):
        a, b, c, d = (uuid.uuid4() for _ in range(4))
        self._befriend(a, b)
        self._befriend(b, c)
        self._befriend(c, d)
        circle = friendship_service.get_friend_circle(a)
        assert circle == {0: [b], 1: [c], 2: [d]}

    def test_circle_is_direction_independent(self):
        # Friendships are stored symmetrically, so the circle is the same
        # regardless of who sent each request.
        a, b, c = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
        friendship_service.send_friend_request(b, a)  # b -> a
        friendship_service.accept_friend_request(b, a)
        friendship_service.send_friend_request(c, b)  # c -> b
        friendship_service.accept_friend_request(c, b)
        circle = friendship_service.get_friend_circle(a)
        assert circle == {0: [b], 1: [c]}
