"""In-memory persistence for ``Friendship`` entities.

Provides ``InMemoryFriendshipRepository``, a dictionary-backed implementation of
``repositories.IRepository`` keyed by the frozenset IDs of Friendship objects.
This stands in for a database-backed repository during early development.
"""

from __future__ import annotations
import datetime
import uuid

import friends.base as base
import repositories


class InMemoryFriendshipRepository(
    repositories.InMemoryRepository[base.FriendshipId, base.Friendship]
):
    """Dictionary backed repository for storing and retrieving Friendship
    entities.
    
    Extends repositories.InMemoryRepository with frozenset[uuid.UUID] as the
    managed type, providing standard CRUD operations scoped to the frozenset
    keys of Friendship objects. Used for integration testing before implementing
    database-backed repositories.
    
    See Also:
        repositories.IRepository
        repositories.InMemoryRepository
    """


# Canonical canned friend graph seeded into every
# InMemoryCannedFriendshipRepository. The two accepted friendships form a chain
#
#     CANNED_FRIEND_SEEKER_ID -- CANNED_DIRECT_FRIEND_ID -- CANNED_RECOMMENDED_FRIEND_ID
#
# so the seeker and the recommended user share a mutual friend but are not
# themselves connected. The seeker id matches the KNOWN_USER_ID used by the
# bridge integration tests.
CANNED_FRIEND_SEEKER_ID = uuid.UUID("11111111-1111-1111-1111-111111111111")
CANNED_DIRECT_FRIEND_ID = uuid.UUID("33333333-3333-3333-3333-333333333333")
CANNED_RECOMMENDED_FRIEND_ID = uuid.UUID("44444444-4444-4444-4444-444444444444")


class InMemoryCannedFriendshipRepository(InMemoryFriendshipRepository):
    """In-memory friendship repository pre-seeded with a small friend graph.

    Behaves exactly like ``InMemoryFriendshipRepository`` but starts populated
    with two accepted friendships forming the chain
    ``CANNED_FRIEND_SEEKER_ID -- CANNED_DIRECT_FRIEND_ID --
    CANNED_RECOMMENDED_FRIEND_ID``. Because the seeker and the recommended user
    share a mutual friend but are not directly connected,
    ``recommendations.find_new_friends(CANNED_FRIEND_SEEKER_ID)`` yields exactly
    ``{CANNED_RECOMMENDED_FRIEND_ID}``. This gives the bridge responder
    deterministic, functional state to serve when exercising the
    GET_RECOMMENDED_FRIENDS path end to end.

    See Also:
        InMemoryFriendshipRepository
    """

    def __init__(self):
        super().__init__()
        now = datetime.datetime.now()
        accepted = base.FriendshipStatus.ACCEPTED
        self.save(
            base.Friendship(
                CANNED_FRIEND_SEEKER_ID, CANNED_DIRECT_FRIEND_ID, now, accepted
            )
        )
        self.save(
            base.Friendship(
                CANNED_DIRECT_FRIEND_ID,
                CANNED_RECOMMENDED_FRIEND_ID,
                now,
                accepted,
            )
        )