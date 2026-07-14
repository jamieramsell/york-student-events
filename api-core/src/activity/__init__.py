"""Minimal activity package for the api-core service.

Exposing a synchronous publish/subscribe registry (``subscribe(listener)`` /
``publish(user_id)``), which lets a given domain slice (e.g. friends) signal
that "this user's facts changed" to any other module(s), without importing said
modules (e.g. badges.badge_service) directly.

This is the trigger mechanism that drives the automatic ``evaluate_badges()``
method- fact-producing slices publish, and the badges slice subscribes.
"""

from activity.base import subscribe, publish

__all__ = [
    "subscribe",
    "publish"
]