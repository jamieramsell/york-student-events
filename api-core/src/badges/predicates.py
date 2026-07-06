"""Composable badge-award predicates for the badges slice.

Defines the rule vocabulary used to decide whether a user has earned a badge.
The design has two layers:

- ``AwardContext`` (with its ``AttendedEvent`` member) is an immutable snapshot
  of a single user's *raw activity facts*: the events they attended, their
  friend count, and the timestamps of messages they sent. It deliberately stores
  raw facts rather than pre-computed aggregates, because each leaf predicate
  derives whatever aggregate it needs (a count, a rolling-window total, ...)
  from those facts at evaluation time. As a result, most new badge ideas can be
  expressed by composing existing leaves and require no change to the context at
  all. When   a genuinely new *fact source* is needed, it should be added as a
  new field with a default value, which keeps every existing caller and stored
  context valid (a non-breaking change).

- ``IPredicate`` and its implementations form a small boolean algebra over that
  context. Leaf predicates (``MinEventsAttended``, ``MinFriends``, ...) test a
  single condition, and the ``&``, ``|`` and ``~`` operators combine them into
  ``AndPredicate`` / ``OrPredicate`` / ``NotPredicate`` trees, so an arbitrarily
  complex award rule is just a nested ``IPredicate``.

Note on ``messages_sent``: it is a stub until the messaging feature exists. It
is modelled as a tuple of send *timestamps* rather than a plain count, so that
re-award windows (``since`` / ``until`` and the rolling-window predicates) can
filter it by time. Once messaging lands, the field can be populated without any
change to the predicates that already consume it.

Caveat on state-style facts and repeatable badges: ``since`` and ``until``
filter the time-stamped facts (``attended_events``, ``messages_sent``) but pass
*state-style* facts such as ``friend_count`` through unchanged, because a scalar
snapshot carries no timestamp to filter on. Therefore, a state-based leaf like
``MinFriends`` should not be the sole condition of a *repeatable* badge- since
its result is independent of the award window, every evaluation would re-satisfy
it and re-award the badge. Pair such a leaf with a time-bounded condition, or
reserve it only for one-shot badges.
"""

from __future__ import annotations

import abc
import dataclasses
import datetime
import uuid

@dataclasses.dataclass(frozen = True)
class AttendedEvent():
    """An immutable record of a single event that a user attended.

    Captures just the facts that the award predicates need to reason about an
    attendance, rather than the full event entity. Used as the element type of
    ``AwardContext.attended_events``.

    Args:
        event_id: The ID of the attended event.
        host_id: The ID of the user or organisation that hosted the event.
        categories: The (possibly empty) set of category tags describing the
            event.
        start_time: The datetime at which the event started, used as the
            attendance's position on the timeline for windowing.
    """
    event_id: uuid.UUID
    host_id: uuid.UUID
    categories: frozenset[str]
    start_time: datetime.datetime


@dataclasses.dataclass(frozen = True)
class AwardContext():
    """An immutable snapshot of one user's raw activity facts.

    - Holds everything that the award predicates evaluate against for a single
    user.
    - Stores *raw* facts (individual attendances, message timestamps) rather
    than pre-computed aggregates, leaving each leaf predicate to derive the
    aggregate that it cares about.
    - New fact sources should be added as new fields with defaults, keeping the
    change non-breaking.

    Note that ``attended_events`` and ``messages_sent`` are time-stamped facts
    that ``since`` and ``until`` filter, whereas ``friend_count`` is a scalar
    state fact that both methods pass through unchanged.

    Args:
        user_id: The ID of the user this context describes.
        attended_events: The events the user attended.
        friend_count: The user's current number of friends. This is a
            point-in-time state value, not a timeline of changes.
        messages_sent: Timestamps of messages that the user has sent. A stub
            until messaging exists. Modelled as timestamps, not a count, so that
            award windows can filter it by time.
    """
    user_id: uuid.UUID
    attended_events: tuple[AttendedEvent, ...] = ()
    friend_count: int = 0
    messages_sent: tuple[datetime.datetime, ...] = ()

    def since(self, cutoff: datetime.datetime) -> AwardContext:
        """Returns a copy of this context keeping only facts at or after a
        cutoff.

        - Filters the time-stamped facts (``attended_events`` (by
        ``start_time``) and ``messages_sent``) to those occurring at or after
        ``cutoff``.
        - State-style facts, such as ``friend_count``, are copied through
        unchanged, since a scalar snapshot has no timestamp to filter on.

        Args:
            cutoff: The inclusive lower bound; facts strictly before it are
                dropped.

        Returns:
            A new ``AwardContext`` containing only the retained facts; the
            original is left unmodified.
        """
        attended_events = tuple(event
                                for event in self.attended_events
                                if event.start_time >= cutoff
                                )
        messages_sent = tuple(message_datetime
                              for message_datetime in self.messages_sent
                              if message_datetime >= cutoff
                              )
        
        return AwardContext(self.user_id,
                            attended_events,
                            self.friend_count,
                            messages_sent
                            )
    
    def until(self, cutoff: datetime.datetime) -> AwardContext:
        """Returns a copy of this context keeping only facts before a cutoff.

        - Filters the time-stamped facts (``attended_events`` (by
        ``start_time``) and ``messages_sent``) to those occurring strictly
        before ``cutoff``.
        - State-style facts such as ``friend_count`` are copied through
        unchanged, since a scalar snapshot has no timestamp to filter on.

        Args:
            cutoff: The exclusive upper bound; facts at or after it are dropped.

        Returns:
            A new ``AwardContext`` containing only the retained facts; the
            original is left unmodified.
        """
        attended_events = tuple(event
                                for event in self.attended_events
                                if event.start_time < cutoff
                                )
        messages_sent = tuple(message_datetime
                              for message_datetime in self.messages_sent
                              if message_datetime < cutoff
                              )
        
        return AwardContext(self.user_id,
                            attended_events,
                            self.friend_count,
                            messages_sent
                            )


class IPredicate(abc.ABC):
    """Interface for a composable badge-award rule.

    A predicate answers a single question about an ``AwardContext``: has this
    condition been met? Concrete leaves implement ``is_satisfied_by()``; the
    ``&``, ``|`` and ``~`` operators combine predicates into boolean trees,
    meaning that a complex award rule is expressed as a nested ``IPredicate``,
    rather than as bespoke logic.
    """

    @abc.abstractmethod
    def is_satisfied_by(self, context: AwardContext) -> bool:
        """Evaluates this predicate against an award context.

        Args:
            context: The snapshot of user activity facts to test.

        Returns:
            Whether the context satisfies this predicate.
        """
        pass

    def __and__(self, other: object) -> AndPredicate:
        """Combines this predicate with another via logical ``AND``.

        Implements the ``&`` operator, e.g. ``a & b``.

        Args:
            other: The predicate to ``AND`` with this one.

        Returns:
            An ``AndPredicate`` satisfied only when both operands are satisfied.

        Raises:
            TypeError: if ``other`` is not an ``IPredicate``.

        See Also:
            AndPredicate
        """
        if not isinstance(other, IPredicate):
            raise TypeError("Predicates can only be compared to eachother")

        return AndPredicate(self, other)

    def __or__(self, other: object) -> OrPredicate:
        """Combines this predicate with another via logical ``OR``.

        Implements the ``|`` operator, e.g. ``a | b``.

        Args:
            other: The predicate to ``OR`` with this one.

        Returns:
            An ``OrPredicate`` satisfied when either operand is satisfied.

        Raises:
            TypeError: if ``other`` is not an ``IPredicate``.

        See Also:
            OrPredicate
        """
        if not isinstance(other, IPredicate):
            raise TypeError("Predicates can only be compared to eachother")

        return OrPredicate(self, other)

    def __invert__(self) -> NotPredicate:
        """Negates this predicate via logical ``NOT``.

        Implements the ``~`` operator, e.g. ``~a``.

        Returns:
            A ``NotPredicate`` satisfied only when this predicate is not.

        See Also:
            NotPredicate
        """
        return NotPredicate(self)


@dataclasses.dataclass(frozen = True)
class AndPredicate(IPredicate):
    """The logical ``AND`` of two predicates.

    Typically constructed via the ``&`` operator rather than directly.

    Args:
        left: The first operand.
        right: The second operand.
    """
    left: IPredicate
    right: IPredicate

    def is_satisfied_by(self, context: AwardContext) -> bool:
        return (self.left.is_satisfied_by(context)
                and self.right.is_satisfied_by(context))


@dataclasses.dataclass(frozen = True)
class OrPredicate(IPredicate):
    """The logical ``OR`` of two predicates.

    Typically constructed via the ``|`` operator rather than directly.

    Args:
        left: The first operand.
        right: The second operand.
    """
    left: IPredicate
    right: IPredicate

    def is_satisfied_by(self, context: AwardContext) -> bool:
        return (self.left.is_satisfied_by(context)
                or self.right.is_satisfied_by(context))


@dataclasses.dataclass(frozen = True)
class NotPredicate(IPredicate):
    """The logical negation of a predicate.

    Typically constructed via the ``~`` operator rather than directly.

    Args:
        operand: The predicate to negate.
    """
    operand: IPredicate

    def is_satisfied_by(self, context: AwardContext) -> bool:
        return not self.operand.is_satisfied_by(context)
    
