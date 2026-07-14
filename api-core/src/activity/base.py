import typing
import uuid

# Listener type alias represents listener methods which recieve a UUID payload
# when publish() is used to release data from the activity package
type Listener = typing.Callable[[uuid.UUID], None]
__listeners: set[Listener] = set()

def subscribe(listener: Listener) -> None:
    """Subscribes a Listener to the activity module, so that it recieves data
    whenever activity.publish() is called.
    """
    __listeners.add(listener)

def publish(user_id: uuid.UUID) -> None:
    """Publishes a ``user_id`` payload to all subscribed listener methods."""
    for listener in __listeners:
        listener(user_id)