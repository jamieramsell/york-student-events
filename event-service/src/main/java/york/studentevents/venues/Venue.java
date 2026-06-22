package york.studentevents.venues;

import java.util.UUID;

/**
 * Represents a venue at which events can be held, identified by name, address, and capacity.
 */
public class Venue implements IVenue {

  private UUID id;
  private String name;
  private String address;
  private int capacity;

  /**
   * Creates a {@code Venue} with the given details.
   *
   * @param name the name of the venue; must not be {@code null} or blank.
   * @param address the address of the venue; must not be {@code null} or blank.
   * @param capacity the maximum number of attendees the venue can hold; must be greater than 0.
   */
  public Venue(String name, String address, int capacity) {
    this(UUID.randomUUID(), name, address, capacity);
  }

  /**
   * Creates a {@code Venue} with the given details.
   *
   * @param id the venue's ID; must not be {@code null}.
   * @param name the name of the venue; must not be {@code null} or blank.
   * @param address the address of the venue; must not be {@code null} or blank.
   * @param capacity the maximum number of attendees the venue can hold; must be greater than 0.
   */
  protected Venue(UUID id, String name, String address, int capacity) {
    if(id == null) {
      throw new IllegalArgumentException("Venue ID cannot be null");
    }
    this.id = id;
    setName(name);
    setAddress(address);
    setCapacity(capacity);
  }

  /**
   * Sets the address of the venue.
   *
   * @param name the new name; must not be {@code null} or blank.
   *
   * @throws IllegalArgumentException if the name is invalid.
   */
  private void setName(String name) {
    if(name == null || name.isBlank()) {
      throw new IllegalArgumentException("Venue name cannot be null, blank, or empty.");
    }
    this.name = name;
  }

  /**
   * Sets the address of the venue.
   *
   * @param address the new address; must not be {@code null} or blank.
   *
   * @throws IllegalArgumentException if the address is invalid.
   */
  private void setAddress(String address) {
    if(address == null || address.isBlank()) {
      throw new IllegalArgumentException("Venue address cannot be null, blank, or empty.");
    }
    this.address = address;
  }

  /**
   * Sets the capacity of the venue.
   *
   * @param capacity the new capacity; must be greater than 0.
   *
   * @throws IllegalArgumentException if the capacity is invalid.
   */
  private void setCapacity(int capacity) {
    if(capacity < 1) {
      throw new IllegalArgumentException("Venue capacity must be greater than zero.");
    }
    this.capacity = capacity;
  }


  /** Returns the unique identifier of the venue. */
  @Override
  public UUID getId() {
    return id;
  }

  /** Returns the name of the venue. */
  @Override
  public String getName() {
    return name;
  }

  /** Returns the address of the venue. */
  @Override
  public String getAddress() {
    return address;
  }

  /** Returns the maximum number of attendees the venue can hold. */
  @Override
  public int getCapacity() {
    return capacity;
  }

  /** Returns a string representation for debugging purposes. */
  @Override
  public String toString() {
    return String.format("Venue[id=%s, name='%s', address='%s', capacity=%d]", id, name, address,
        capacity);
  }
}
