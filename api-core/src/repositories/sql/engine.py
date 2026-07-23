import os
import sqlalchemy

def create_engine_from_env(var: str = "DATABASE_URL") -> sqlalchemy.Engine:
    """Utility function which retrieves the URL pointed at by an env var and
    uses it to create an sqlalchemy Engine.
    
    Args:
        var: The environment variable which contains the URL of the database.
        
    Returns:
        The sqlalchemy.Engine of the given database.
        
    Raises:
        ValueError: if the given env var was not found.
    """
    env_var = os.getenv(var)
    if env_var is None:
        raise ValueError("No environment variable named " + var + " was found.")
    
    return sqlalchemy.create_engine(env_var)