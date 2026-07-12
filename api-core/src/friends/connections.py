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

    # Base case: implicit. The function stops recursing when there are no mpre
    # unvisited friends.
    for friend in friends_of_user:
        if friend not in visited:
            visited = visited.union(dfs_recursion(friend, visited))

    return visited
            
