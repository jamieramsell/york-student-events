"""Synchronous publish/subscribe registry backing the ``activity`` package.

Holds the module-level set of subscribed listeners and the ``subscribe`` /
``publish`` primitives that ``activity`` re-exports. A fact-producing slice
publishes a ``user_id`` and every subscribed listener is invoked synchronously,
so slices can signal "this user's facts changed" without importing one another.
"""

import typing
import uuid

# Listener type alias represents listener methods which receive a UUID payload
# when publish() is used to release data from the activity package
type Listener = typing.Callable[[uuid.UUID], None]
__listeners: set[Listener] = set()

def subscribe(listener: Listener) -> None:
    """Subscribes a Listener to the activity module, so that it receives data
    whenever activity.publish() is called.
    """
    __listeners.add(listener)

def publish(user_id: uuid.UUID) -> None:
    """Publishes a ``user_id`` payload to all subscribed listener methods.
    
    Note that a listener raising an exception propagates to the publisher,
    meaning that the publisher should be equipped to deal with these exceptions;
    they are not hidden or handled here.
    """
    for listener in __listeners:
        listener(user_id)