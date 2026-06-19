import sys
import json


def get_user_badges(payload):
    #TODO
    return {"badges": ["First Event", "Social5"]}

def get_user_friends(payload):
    #TODO
    return "TEMP FRIEND"

def award_badge(payload):
    #TODO
    raise ValueError("THIS IS A TEST ERROR")

class MessageHandlerFactory:
    def __init__(self):
        self._handlers = {
            "GET_USER_BADGES": get_user_badges,
            "GET_USER_FRIENDS": get_user_friends,
            "AWARD_BADGE": award_badge
        }

    def get_handler(self, message_type):
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

                handler = factory.get_handler(line)
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

