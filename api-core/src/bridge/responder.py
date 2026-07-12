import attendance
import collections.abc
import json
import sys
import uuid

# Type alias of a Payload returned by a handler, formed of str keys, and
# list[str] elements
type Payload = dict[str, list[str]]

# Type alias of a callable Handler, which accepts a str as a parameter, and
# returns a Payload
type Handler = collections.abc.Callable[[str], Payload]

#TODO
def get_user_badges(payload: str):
    return {"badges": ["First Event", "Social5"]}

#TODO
def get_user_friends(payload: str):
    return {"friends": ["James", "Jamie"]}

#TODO
def award_badge(payload: str):
    raise ValueError("THIS IS A TEST ERROR")

#TODO
def get_recommended_events(payload: str):
    return {"events": ["cd1e0662-beab-4fc0-af84-9dc29c98d561",
                       "0c51b12f-6bec-4172-bba0-25bba3bef9d9"]}


def record_attendance(payload: dict) -> Payload:
    attendee_id = uuid.UUID(payload["userId"])
    event_id = uuid.UUID(payload["eventId"])
    attendance.record_attendance(attendee_id, event_id)
    return {}


class MessageHandlerFactory:
    """Routes incoming messages to their corresponding handler functions.

    Attributes:
        _handlers (dict[str, Callable): An internal mapping of supported
            message types and their handler functions.
    """
    def __init__(self):
        self._handlers: dict[str, Handler] = {
            "GET_USER_BADGES": get_user_badges,
            "GET_USER_FRIENDS": get_user_friends,
            "AWARD_BADGE": award_badge,
            "GET_RECOMMENDED_EVENTS": get_recommended_events,
            "RECORD_ATTENDANCE": record_attendance
        }

    def get_handler(self, message_type: str) -> Handler:
        """Fetches the correct message handler from the message type provided.

        Args:
            message_type (str): String defining the type of request being
                passed. Currently supported types are:
                - `GET_USER_BADGES`,
                - `GET_USER_FRIENDS`,
                - `AWARD_BADGE`,
                - `GET_RECOMMENDED_EVENTS`,
                - `RECORD_ATTENDANCE`

        Raises:
            ValueError: If the message type is unknown, or doesn't have a
                corresponding handler.

        Returns:
            Callable: The function or method assigned to handle the
                message type.
        """
        handler = self._handlers.get(message_type)
        if not handler:
            raise ValueError(f"Unable to find handler for: `{message_type}`.")
        return handler


def main():
    factory = MessageHandlerFactory()

    for line in sys.stdin:

        line = line.strip()
        if not line:
            continue

        try:
            request = json.loads(line)
            msg_type = request.get("requestType")
            payload = request.get("payload", {})

            if not msg_type:
                raise ValueError("Missing 'requestType' field.")

            handler = factory.get_handler(msg_type)
            result_payload = handler(payload)

            response: dict[str, str | Payload] = {
                "status": "ok",
                "payload": result_payload,
            }

        except json.JSONDecodeError:
            response = {
                "status": "error",
                "error": "Incorrectly formated json."
            }

        except Exception as e:
            response = {
                "status": "error",
                "error": str(e)
            }

        sys.stdout.write(json.dumps(response) + "\n")
        sys.stdout.flush()

        if response["status"] == "error":
            sys.exit(1)

if __name__ == "__main__":
    main()