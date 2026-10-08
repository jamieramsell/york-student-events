package york.studentevents.venues;

import york.studentevents.repository.IEntity;

/** Represents a venue where events can be held. */
public abstract class IVenue extends IEntity {

  /** Returns the name of the venue. */
  public abstract String getName();

  /** Returns the address of the venue. */
  public abstract String getAddress();

  /**
   * Returns the maximum number of attendees the venue can hold.
   *
   * @return the capacity of the venue, or {@code null} if the venue has no maximum capacity.
   */
  public abstract Integer getCapacity();

  /**
   * Sets the address of the venue.
   *
   * @param name the new name.
   *
   * @throws IllegalArgumentException if the name is {@code null}, blank, or empty.
   */
  public abstract void setName(String name);

  /**
   * Sets the address of the venue.
   *
   * @param address the new address.
   *
   * @throws IllegalArgumentException if the address is {@code null}, blank, or empty.
   */
  public abstract void setAddress(String address);

  /**
   * Sets the capacity of the venue.
   *
   * @param capacity the new capacity; must be greater than 0, or {@code null} if the venue has no
   *     maximum capacity.
   *
   * @throws IllegalArgumentException if capacity {@code <= 0}, and is not {@code null}.
   */
  public abstract void setCapacity(Integer capacity);

}
