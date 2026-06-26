from dataclasses import dataclass
from datetime import datetime

@dataclass
class Friendship:
    """
    Defines the core structure for friendship service relationship in the api-core service.
    """
    user_id: int
    friend_id: int
    created_at: datetime
