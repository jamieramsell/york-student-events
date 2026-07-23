"""Smoke test: prove api-core can connect to its database and round-trip a row.

Reads the connection URL from the ``DATABASE_URL`` environment variable (via
``repositories.sql.create_engine_from_env``), inserts a throwaway ``attendance``
row, reads it back on a *fresh* connection to prove it was genuinely persisted
rather than merely held in the writing transaction, then deletes it again.
Prints ``OK`` and exits 0 on success; prints a diagnostic and exits 1 otherwise.

Usage (from ``api-core/``, with the database up and ``DATABASE_URL`` exported,
and the schema created via ``alembic upgrade head``)::

    python scripts/check_db.py
"""

from __future__ import annotations

import datetime
import os
import sys
import uuid

# scripts/ lives beside src/, not inside it, so put the source root on the path
# the same way tests/conftest.py and alembic/env.py do.
_SRC = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "src"))
if _SRC not in sys.path:
    sys.path.insert(0, _SRC)

import repositories.sql as sql  # noqa: E402  (import after sys.path is set)


def main() -> int:
    engine = sql.create_engine_from_env()

    attendee_id = uuid.uuid4()
    event_id = uuid.uuid4()
    row_id = (sql.attendance.c.attendee_id == attendee_id) & (
        sql.attendance.c.event_id == event_id
    )

    # Write the throwaway row in its own transaction...
    with engine.begin() as conn:
        conn.execute(
            sql.attendance.insert().values(
                attendee_id=attendee_id,
                event_id=event_id,
                recorded_at=datetime.datetime.now(datetime.timezone.utc),
            )
        )

    # ...then read it back on a fresh connection, proving it survived the commit.
    with engine.connect() as conn:
        row = conn.execute(sql.attendance.select().where(row_id)).one_or_none()

    # Always remove the throwaway row, whether or not the read above succeeded.
    with engine.begin() as conn:
        conn.execute(sql.attendance.delete().where(row_id))

    if row is None:
        print("FAILED: row was written but could not be read back")
        return 1

    print("OK: round-tripped a row via", engine.url.render_as_string(
        hide_password=True
    ))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
