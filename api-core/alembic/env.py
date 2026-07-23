"""Alembic migration environment for api-core.

The connection URL is never stored in ``alembic.ini``; it is read from the
``DATABASE_URL`` environment variable (the same variable the application reads
via ``repositories.sql.create_engine_from_env``), so migrations run against
whichever database the surrounding environment is configured for and no
credentials live in version control.

``target_metadata`` points at the shared ``repositories.sql`` metadata, so
``alembic revision --autogenerate`` compares against the one schema definition
the concrete repositories also bind to.
"""

import os
import sys
from logging.config import fileConfig

from alembic import context

# The api-core modules use package-qualified imports (e.g.
# ``import repositories.sql``) and expect the ``src`` source root on sys.path,
# exactly as tests/conftest.py arranges it for the test suite.
_SRC = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "src"))
if _SRC not in sys.path:
    sys.path.insert(0, _SRC)

import repositories.sql as sql  # noqa: E402  (import after sys.path is set)

# this is the Alembic Config object, which provides
# access to the values within the .ini file in use.
config = context.config

# Interpret the config file for Python logging.
# This line sets up loggers basically.
if config.config_file_name is not None:
    fileConfig(config.config_file_name)

# The one source of truth for the api-core schema; drives --autogenerate.
target_metadata = sql.metadata


def run_migrations_offline() -> None:
    """Run migrations in 'offline' mode.

    This configures the context with just a URL
    and not an Engine, though an Engine is acceptable
    here as well.  By skipping the Engine creation
    we don't even need a DBAPI to be available.

    Calls to context.execute() here emit the given string to the
    script output.

    """
    url = os.environ["DATABASE_URL"]
    context.configure(
        url=url,
        target_metadata=target_metadata,
        literal_binds=True,
        dialect_opts={"paramstyle": "named"},
    )

    with context.begin_transaction():
        context.run_migrations()


def run_migrations_online() -> None:
    """Run migrations in 'online' mode.

    In this scenario we need to create an Engine
    and associate a connection with the context.

    """
    connectable = sql.create_engine_from_env()

    with connectable.connect() as connection:
        context.configure(
            connection=connection, target_metadata=target_metadata
        )

        with context.begin_transaction():
            context.run_migrations()


if context.is_offline_mode():
    run_migrations_offline()
else:
    run_migrations_online()
