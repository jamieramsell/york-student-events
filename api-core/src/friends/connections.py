"""Graph traversal algorithms for the friends slice.
"""

import friends.friendship_service
import uuid

def dfs_recursion(start_node: uuid.UUID,
                  visited: set[uuid.UUID]) -> set[uuid.UUID]:
    """Utility function which traverses a friendship graph depth-first, starting
    from a given node (user), and finding their connected friends.

    Args:
        start_node: the node (user) to begin the search from.
        visited: the set of nodes (users) that have already been explored.

    Returns:
        The union of the set of visited nodes, and the set of the friends of the
        start_node.
    """
    visited = visited.copy()
    visited.add(start_node)
    friends_of_user = friends.friendship_service.get_friends(start_node)

    # Base case: implicit. The function stops recursing when there are no more
    # unvisited friends.
    for friend in friends_of_user:
        if friend not in visited:
            visited = visited.union(dfs_recursion(friend, visited))

    return visited


def bfs_friend_layers(
    start_node: uuid.UUID,
    max_layers: int | None = None
) -> dict[int, set[uuid.UUID]]:
    """Traverses a friendship graph breadth-first, grouping reachable users by
    their distance from the start node.

    Layer 0 holds the direct friends of ``start_node``, layer 1 holds users who
    are friends of those friends (but not already direct friends), and so on.
    ``start_node`` itself never appears in any layer, and each user appears only
    in the earliest (closest) layer that reaches them.

    Args:
        start_node: the node (user) to begin the search from.
        max_layers: the highest layer index to generate; ``None`` traverses
            until the graph is exhausted. A value of ``1`` yields layers 0 and
            1.

    Returns:
        A dictionary mapping each layer index to the set of user IDs at that
        distance. Never ``None``, but may be empty (e.g. a user with no
        friends).
    """

    layers: dict[int, set[uuid.UUID]] = {}

    # The start node is marked visited up front so it can never reappear as one
    # of its own (possibly transitive) friends.
    visited: set[uuid.UUID] = {start_node}
    frontier: set[uuid.UUID] = {start_node}
    current_layer = 0

    while frontier:

        # Collect the not-yet-seen friends of everyone in the current frontier;
        # these form the next layer out from the start node.
        next_frontier: set[uuid.UUID] = set()
        for node in frontier:
            for friend in friends.friendship_service.get_friends(node):
                if friend not in visited:
                    visited.add(friend)
                    next_frontier.add(friend)

        # No new users were reached, so the graph is exhausted.
        if not next_frontier:
            break

        layers[current_layer] = next_frontier

        # Stop once the requested depth has been produced.
        if max_layers is not None and current_layer >= max_layers:
            break

        frontier = next_frontier
        current_layer += 1

    return layers

