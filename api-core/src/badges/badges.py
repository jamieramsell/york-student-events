from dataclasses import dataclass

@dataclass
class Badge:
    """
    Defines the core data structures for a badge in the Python api-core service
    """

    id: int
    award_condition: str #to be made more structure at M4
    name: str
    description: str
