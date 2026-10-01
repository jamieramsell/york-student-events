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
import typing
import uuid

# Stores a dictionary, keyed by serialisation tags, which stores each
# implementation of IPredicate. Automatically populated by the use of the
# @_register tag.
_PREDICATE_REGISTRY: dict[str, type[IPredicate]] = {}

# Use of generic type P, extends IPredicate, to ensure that @_register doesn't
# narrow a child class of IPredicate back down
_P = typing.TypeVar("_P", bound = type["IPredicate"])

def _register(tag: str) -> typing.Callable[[_P], _P]:
    """Class decorator binding a predicate class to its serialisation tag.

    Records the class in the module registry and stores the tag on the class so
    that ``to_dict`` can emit it, keeping the tag defined in exactly one place.
    """
    def decorator(cls: _P) -> _P:
        cls.type_tag = tag
        _PREDICATE_REGISTRY[tag] = cls
        return cls
    
    return decorator


@dataclasses.dataclass(frozen = True)
class AttendedEvent:
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
class AwardContext:
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


# Type aliases, representing the union of json serialisable types, and the union
# of predicate property which can be expressed as json serialisable types
# respectively.
type JsonSerialisable = (dict[str, JsonSerialisable]
                         | list[JsonSerialisable]
                         | str
                         | int
                         | float
                         | bool
                         | None)

type PredicateProperty = (IPredicate
                          | uuid.UUID
                          | datetime.datetime
                          | datetime.timedelta
                          | tuple[PredicateProperty, ...]
                          | int
                          | str
                          | None)

def _to_jsonable(value: PredicateProperty) -> JsonSerialisable:
    """Helper function used to return the jsonable format of a given property of
    an IPredicate."""
    match value:
        case IPredicate():
            return value.to_dict()
        case uuid.UUID():
            return str(value)
        case datetime.datetime():
            return value.isoformat()
        case datetime.timedelta():
            return value.total_seconds()
        case tuple():
            return [_to_jsonable(v) for v in value]
        case int() | str() | None:
            # Scalars that are already JSON-native (``threshold``, ``category``,
            # and the ``None`` of an unset optional) pass straight through.
            return value
        case _: # Default case should never be reached
            raise TypeError(f"Illegal `value` argument of type {type(value)}.")


def _build_between_from_dict(
    raw: list[str | None] | None,
) -> tuple[datetime.datetime | None, datetime.datetime | None] | None:
    """Rebuilds a ``between`` bound-pair from its serialised form.

    The inverse of serialising a ``between`` tuple via ``_to_jsonable``: an
    absent window stays ``None``, and each ISO-8601 bound is parsed back to a
    ``datetime`` (a ``None`` bound is left open). The result is rebuilt as a
    ``tuple`` so that a round-tripped predicate compares equal to the original.

    Args:
        raw: ``None`` for no window, otherwise a ``[start, end]`` list whose
            entries are ISO-8601 strings or ``None``.

    Returns:
        The reconstructed ``(start, end)`` tuple, or ``None``.
    """
    if raw is None:
        return None

    start, end = raw
    return (
        datetime.datetime.fromisoformat(start) if start is not None else None,
        datetime.datetime.fromisoformat(end) if end is not None else None,
    )


class IPredicate(abc.ABC):
    """Interface for a composable badge-award rule.

    A predicate answers a single question about an ``AwardContext``: has this
    condition been met? Concrete leaves implement ``is_satisfied_by()``; the
    ``&``, ``|`` and ``~`` operators combine predicates into boolean trees,
    meaning that a complex award rule is expressed as a nested ``IPredicate``,
    rather than as bespoke logic.
    """

    type_tag: typing.ClassVar[str] # set by @_register on each concrete subclass

    @abc.abstractmethod
    def is_satisfied_by(self, context: AwardContext) -> bool:
        """Evaluates this predicate against an award context.

        Args:
            context: The snapshot of user activity facts to test.

        Returns:
            Whether the context satisfies this predicate.
        """
        ...

    def to_dict(self) -> dict[str, JsonSerialisable]:
        """Serialises this predicate to a JSON-ready, type-tagged dict.

        Emits the class's ``type_tag`` under the ``"type"`` key, then each
        dataclass field converted to a JSON-native value by ``_to_jsonable``
        (combinators thereby nest their operands recursively). The result is the
        exact input that ``predicate_from_dict`` consumes to rebuild the tree.

        Returns:
            A dict of the ``"type"`` tag plus this predicate's parameters.
        """
        # Every concrete predicate is a frozen dataclass; the base itself is not,
        # so assert the invariant to read its fields (and satisfy the checker).
        assert dataclasses.is_dataclass(self)

        data: dict[str, JsonSerialisable] = {"type": self.type_tag}
        for field in dataclasses.fields(self):
            data[field.name] = _to_jsonable(getattr(self, field.name))

        return data

    @classmethod
    @abc.abstractmethod
    def from_dict(cls, data: dict[str, typing.Any]) -> IPredicate:
        """Rebuilds a predicate of this type from its serialised dict.

        The inverse of ``to_dict``: each concrete class reads its own parameters
        back off ``data``, converting them to their real types (and recursing
        via ``predicate_from_dict`` for operands). Callers typically use the
        module-level ``predicate_from_dict`` router rather than dispatching here
        directly.

        Args:
            data: A tagged dict previously produced by ``to_dict``.

        Returns:
            The reconstructed predicate.
        """
        ...

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


def predicate_from_dict(data: dict[str, typing.Any]) -> IPredicate:
    """Rebuilds a predicate tree from its serialised, type-tagged dict.

    Looks up the ``"type"`` tag in the module registry and delegates to that
    class's ``from_dict``. Because dispatch is table-driven, new predicate types
    become deserialisable simply by being registered- there is no ``if``/``elif``
    chain to extend. This is the inverse of ``IPredicate.to_dict``, and the two
    round-trip: ``predicate_from_dict(p.to_dict()) == p``.

    Args:
        data: A tagged dict previously produced by ``to_dict``.

    Returns:
        The reconstructed predicate.

    Raises:
        ValueError: if the ``"type"`` tag is missing or is not registered.
    """
    tag = data.get("type")
    cls = _PREDICATE_REGISTRY.get(tag) if isinstance(tag, str) else None

    if cls is None:
        raise ValueError(f"Unknown predicate type tag: {tag!r}")

    return cls.from_dict(data)


@_register("and")
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

    @classmethod
    def from_dict(cls, data: dict[str, typing.Any]) -> AndPredicate:
        return cls(left = predicate_from_dict(data["left"]),
                   right = predicate_from_dict(data["right"]))


@_register("or")
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

    @classmethod
    def from_dict(cls, data: dict[str, typing.Any]) -> OrPredicate:
        return cls(left = predicate_from_dict(data["left"]),
                   right = predicate_from_dict(data["right"]))


@_register("not")
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

    @classmethod
    def from_dict(cls, data: dict[str, typing.Any]) -> NotPredicate:
        return cls(operand = predicate_from_dict(data["operand"]))
    

@_register("min_events_attended")
@dataclasses.dataclass(frozen = True)
class MinEventsAttended(IPredicate):
    """Satisfied once the user has attended at least ``threshold`` events.

    Counts attendances in the context, optionally narrowed to a single host, a
    single category, and/or a time window, and passes once the count reaches the
    threshold.

    Args:
        threshold: The minimum number of matching attendances required.
        host_id: If set, only events hosted by this host are counted.
        category: If set, only events tagged with this category are counted.
        between: If set, a ``(start, end)`` pair bounding the window to
            consider; either bound may be ``None`` to leave that side open.
            ``start`` is applied via ``AwardContext.since`` (inclusive), and
            ``end`` via ``AwardContext.until`` (exclusive).
    """
    threshold: int
    host_id: uuid.UUID | None = None
    category: str | None = None
    between: tuple[datetime.datetime | None,
                   datetime.datetime | None] | None = None

    def is_satisfied_by(self, context: AwardContext) -> bool:

        # If cutoff datetimes are specified, apply them to the award context
        if self.between is not None:
            start = self.between[0]
            end = self.between[1]

            if start is not None:
                context = context.since(start)
            
            if end is not None:
                context = context.until(end)

        # Counter to keep track of the number of events attended within the
        # award context which align with the restrictions applied by this
        # instance of predicate
        valid_events_attended = 0

        for event in context.attended_events:
            
            if self.host_id is not None and self.host_id != event.host_id:
                continue

            if (
                self.category is not None
                and self.category not in event.categories
            ):
                continue

            # If this point is reached, then the current event meets all
            # restrictions; increment the counter!
            valid_events_attended += 1
            
            # Check whether the loop can be terminated early- there's no point
            # in looping further if the threshold has already been reached
            if valid_events_attended >= self.threshold:
                return True
        
        # If we finish looping and the program hasn't yet returned, then the
        # predicate has not been satisfied by the given award context.
        return False

    @classmethod
    def from_dict(cls, data: dict[str, typing.Any]) -> MinEventsAttended:
        host_id = data["host_id"]
        return cls(
            threshold = data["threshold"],
            host_id = uuid.UUID(host_id) if host_id is not None else None,
            category = data["category"],
            between = _build_between_from_dict(data["between"]),
        )


@_register("min_events_in_rolling_window")
@dataclasses.dataclass(frozen = True)
class MinEventsInRollingWindow(IPredicate):
    """Satisfied if any ``window``-long span contains ``threshold`` events.

    Unlike ``MinEventsAttended``, which counts over a fixed window, this slides
    a window of fixed *duration* across the timeline of attendances and passes
    if the window ever holds enough matching events at once, capturing bursts
    of activity regardless of when they occur.

    Args:
        threshold: The minimum number of matching attendances required within a
            single window.
        window: The duration of the sliding window.
        host_id: If set, only events hosted by this host are counted.
        category: If set, only events tagged with this category are counted.
    """
    threshold: int
    window: datetime.timedelta
    host_id: uuid.UUID | None = None
    category: str | None = None

    def __single_event_satisfies_predicate(self, event: AttendedEvent) -> bool:
        """Helper method which determines whether a single ``AttendedEvent``
        satisfies this predicate instance.
        
        Note that the method assumes that the event has already been checked
        to see whether it does indeed fall within the current rolling window.
        
        Args:
            event: The ``AttendedEvent`` to verify against the requirements of
                the predicate.
        
        Returns:
            ``True`` if the event meets the requirements of the predicate;
            otherwise ``False``.
        """
        if self.host_id is not None and self.host_id != event.host_id:
            return False

        # The sequential guard clauses read more clearly than a single negated
        # boolean expression as more restrictions are added.
        if self.category is not None and self.category not in event.categories:  # noqa: SIM103
            return False

        # If this point is reached, then the current event meets all
        # restrictions
        return True

    def is_satisfied_by(self, context: AwardContext) -> bool:

        # Counter to keep track of the number of events attended within the
        # award context which align with the restrictions applied by this
        # instance of predicate
        valid_events_attended = 0

        # Two variables to follow the rolling window:
        # - index points to the event at the beginning of the rolling window
        # - rolling_window_start represents the start datetime of that event
        index_of_event_at_rolling_window_start = 0
        rolling_window_start: datetime.datetime | None = None

        # Sort the events within the award context chronologically to
        # facilitate the rolling window
        chronological_events = list(context.attended_events)
        chronological_events.sort(key = lambda event: event.start_time)

        for event in chronological_events:
            
            # Begin with the first event attended within the award context
            if rolling_window_start is None:
                rolling_window_start = event.start_time
                
            # If the current event exceeds the time frame of the rolling window,
            # then slide the rolling window to begin at the time of the next
            # event in the given award context. Keep sliding until the current
            # event does indeed fall within the rolling window.
            while rolling_window_start + self.window < event.start_time:
                
                # If the event at the beginning of the window (which is about to
                # be excluded from it) is valid, then it must be discounted from
                # the counter
                if self.__single_event_satisfies_predicate(
                    chronological_events[index_of_event_at_rolling_window_start]
                ):
                    valid_events_attended -= 1

                # Slide the rolling window by incrementing the index pointer,
                # then advance its start to the new left-edge event so the loop
                # condition re-evaluates (and terminates). The pointer can never
                # overtake the current event: once it reaches it,
                # ``rolling_window_start`` equals ``event.start_time`` and the
                # condition becomes false.
                index_of_event_at_rolling_window_start += 1
                rolling_window_start = chronological_events[
                    index_of_event_at_rolling_window_start
                ].start_time

            # By this point, we have determined that the current event does
            # indeed fall within the rolling window.

            if self.__single_event_satisfies_predicate(event):
                valid_events_attended += 1
            
            # Check whether the loop can be terminated early- there's no point
            # in looping further if the threshold has already been reached
            if valid_events_attended >= self.threshold:
                return True
        
        # If we finish looping and the program hasn't yet returned, then the
        # predicate has not been satisfied by the given award context.
        return False

    @classmethod
    def from_dict(cls, data: dict[str, typing.Any]) -> MinEventsInRollingWindow:
        host_id = data["host_id"]
        return cls(
            threshold = data["threshold"],
            window = datetime.timedelta(seconds = data["window"]),
            host_id = uuid.UUID(host_id) if host_id is not None else None,
            category = data["category"],
        )


@_register("min_friends")
@dataclasses.dataclass(frozen = True)
class MinFriends(IPredicate):
    """Satisfied once the user has at least ``threshold`` friends.

    Note that ``friend_count`` is a state-style fact that ``since`` and
    ``until`` pass through unchanged, so this leaf's result is independent of any
    award window. It should therefore not be the sole condition of a *repeatable*
    badge -- every evaluation would re-satisfy it and re-award the badge. Pair it
    with a time-bounded condition, or use it only for one-shot badges.

    Args:
        threshold: The minimum number of friends required.
    """
    threshold: int

    def is_satisfied_by(self, context: AwardContext) -> bool:
        """Evaluates the friend-count rule against an award context.

        Args:
            context: The snapshot of user activity facts to test.

        Returns:
            ``True`` if the user's ``friend_count`` meets ``threshold``.
        """
        return context.friend_count >= self.threshold

    @classmethod
    def from_dict(cls, data: dict[str, typing.Any]) -> MinFriends:
        return cls(threshold = data["threshold"])
    

@_register("min_messages_sent")
@dataclasses.dataclass(frozen = True)
class MinMessagesSent(IPredicate):
    """Satisfied once the user has sent at least ``threshold`` messages.

    Counts message timestamps in the context, optionally narrowed to a time
    window.
    
    Relies on ``AwardContext.messages_sent``, which is a stub until messaging
    exists, so this predicate is inert until that feature lands.

    Args:
        threshold: The minimum number of messages required.
        between: If set, a ``(start, end)`` pair bounding the window to
            consider; either bound may be ``None`` to leave that side open.
            ``start`` is applied via ``AwardContext.since`` (inclusive), and
            ``end`` via ``AwardContext.until`` (exclusive).
    """
    threshold: int
    between: tuple[datetime.datetime | None,
                   datetime.datetime | None] | None = None

    def is_satisfied_by(self, context: AwardContext) -> bool:

        # If cutoff datetimes are specified, apply them to the award context
        if self.between is not None:
            start = self.between[0]
            end = self.between[1]

            if start is not None:
                context = context.since(start)
            
            if end is not None:
                context = context.until(end)

        return len(context.messages_sent) >= self.threshold

    @classmethod
    def from_dict(cls, data: dict[str, typing.Any]) -> MinMessagesSent:
        return cls(
            threshold = data["threshold"],
            between = _build_between_from_dict(data["between"]),
        )


@_register("min_messages_sent_in_rolling_window")
@dataclasses.dataclass(frozen = True)
class MinMessagesSentInRollingWindow(IPredicate):
    """Satisfied if any ``window``-long span contains ``threshold`` messages.

    Unlike ``MinMessagesSent``, which counts over a fixed window, this slides
    a window of fixed *duration* across the timeline of messages and passes
    if the window ever holds enough messages at once, capturing bursts of
    activity regardless of when they occur.
    
    Relies on ``AwardContext.messages_sent``, which is a stub until messaging
    exists, so this predicate is inert until that feature lands.

    Args:
        threshold: The minimum number of messages required within a single
            window.
        window: The duration of the sliding window.
    """
    threshold: int
    window: datetime.timedelta

    def is_satisfied_by(self, context: AwardContext) -> bool:

        # If less messages than the threshold have been sent within the given
        # context, or none have been sent at all, then return False
        if (
            len(context.messages_sent) < self.threshold
            or len(context.messages_sent) == 0
        ):
            return False
        
        # Sort messages within the given award context chronologically to
        # facilitate the rolling window
        chronological_messages: list[datetime.datetime] = list(
            context.messages_sent
        )
        chronological_messages.sort()

        # Beginning with the first message sent within the given award context,
        # slide the rolling window to begin at the time that each message was
        # sent. Keep sliding until a rolling window is found with a sufficient
        # number of messages sent to satisfy the predicate.
        for message_timestamp in chronological_messages:

            start: datetime.datetime = message_timestamp

            current_window_of_context = context.since(start)
            current_window_of_context = current_window_of_context.until(
                start + self.window
            )

            if len(current_window_of_context.messages_sent) >= self.threshold:
                return True

        return False

    @classmethod
    def from_dict(
        cls, data: dict[str, typing.Any]
    ) -> MinMessagesSentInRollingWindow:
        return cls(
            threshold = data["threshold"],
            window = datetime.timedelta(seconds = data["window"]),
        )