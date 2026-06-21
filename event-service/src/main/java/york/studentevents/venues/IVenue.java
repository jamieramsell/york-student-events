package york.studentevents.venues;

import york.studentevents.repository.IEntity;

/** Represents a venue where events can be held. */
public interface IVenue extends IEntity {

  /** Returns the name of the venue. */
  String getName();

  /** Returns the address of the venue. */
  String getAddress();

  /** Returns the maximum number of attendees the venue can hold. */
  int getCapacity();

}
