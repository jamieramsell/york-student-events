package york.studentevents.venues;

/** Represents a venue where events can be held. */
public interface IVenue {

  /** Returns the unique identifier of the venue. */
  long getId();

  String getName();

  String getAddress();

  /** Returns the maximum number of attendees the venue can hold. */
  int getCapacity();

}
