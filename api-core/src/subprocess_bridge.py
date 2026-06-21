import sys
import json

#TODO
def get_user_badges(payload):
    return {"badges": ["First Event", "Social5"]}

#TODO
def get_user_friends(payload):
    return "TEMP FRIEND"

#TODO
def award_badge(payload):
    raise ValueError("THIS IS A TEST ERROR")

class MessageHandlerFactory:
    """
        Routes incoming messages to their corresponding handler functions.

        Attributes:
            _handlers (dict[str, Callable): An internal mapping of supported
                message types and their handler functions.
    """
    def __init__(self):
        self._handlers = {
            "GET_USER_BADGES": get_user_badges,
            "GET_USER_FRIENDS": get_user_friends,
            "AWARD_BADGE": award_badge
        }

    def get_handler(self, message_type):
        """
            Fetches the correct message handler from the message type provided.

            Args:
                message_type (str): String defining the type of request being
                    passed. Currently supported types are:
                        `GET_USER_BADGES`, `GET_USER_FRIENDS`, and `AWARD_BADGE`.

            Raises:
                ValueError: If the message type is unknown or doesn't have a
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
                msg_type = request.get("type")
                payload = request.get("payload", {})

                if not msg_type:
                    raise ValueError("Missing 'type' field.")

                handler = factory.get_handler(msg_type)
                result_payload = handler(payload)

                response = {
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

if __name__ == "__main__":
    main()