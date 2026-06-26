"""Shared pytest configuration for the api-core test suite.

Puts ``api-core/src`` on ``sys.path`` so tests can import packages such as
``bridge`` and ``repositories`` the same way the service code does.
"""
import sys
from pathlib import Path

_SRC = Path(__file__).resolve().parents[1] / "src"
if str(_SRC) not in sys.path:
    sys.path.insert(0, str(_SRC))
