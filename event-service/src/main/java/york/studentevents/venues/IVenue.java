package york.studentevents.venues;

import java.util.UUID;

/** Represents a venue where events can be held. */
public interface IVenue {

  /** Returns the unique identifier of the venue. */
  UUID getId();

  /** Returns the name of the venue. */
  String getName();

  /** Returns the address of the venue. */
  String getAddress();

  /** Returns the maximum number of attendees the venue can hold. */
  int getCapacity();

}
