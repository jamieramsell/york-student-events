package york.studentevents.venues;

/** Represents a venue where events can be held. */
public interface IVenue {

  /** Returns the unique identifier of the venue. */
  public long getId();

  public String getName();


  public String getAddress();

  /** Returns the maximum number of attendees the venue can hold. */
  public int getCapacity();

}
