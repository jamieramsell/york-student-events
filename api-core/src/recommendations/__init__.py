"""Recommendations slice for the api-core service.

This package defines the algorithms for the events app recommendations, such as
those for finding new events and new friends. The public surface is the
service-level operations callers use to execute the algorithms and find
recommendations for a given user.
"""

from recommendations.base import get_recommended_events, find_new_friends

__all__ = [
    "get_recommended_events",
    "find_new_friends"
]
