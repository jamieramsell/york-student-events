package york.studentevents.venues;

import java.util.UUID;

/**
 * Represents a venue at which events can be held, identified by name, address, and capacity.
 */
public class Venue implements IVenue {

  private UUID id;
  private String name;
  private String address;
  private Integer capacity;

  /**
   * Creates a {@code Venue} with a maximum attendee capacity.
   *
   * @param name the name of the venue.
   * @param address the address of the venue.
   * @param capacity the maximum number of attendees the venue can hold.
   * 
   * @throws IllegalArgumentException if any of the parameters are {@code null}, blank, or empty, or
   *      if {@code capacity} is less than one.
   */
  public Venue(String name, String address, int capacity) {
    this(UUID.randomUUID(), name, address, capacity);
  }

  /**
   * Creates a {@code Venue} with a specific ID value and a maximum attendee capacity.
   *
   * @param id the venue's ID.
   * @param name the name of the venue.
   * @param address the address of the venue.
   * @param capacity the maximum number of attendees the venue can hold.
   * 
   * @throws IllegalArgumentException if any of the parameters are {@code null}, blank, or empty, or
   *      if {@code capacity} is less than one.
   */
  protected Venue(UUID id, String name, String address, int capacity) {
    if (id == null) {
      throw new IllegalArgumentException("Venue ID cannot be null");
    }
    this.id = id;
    setName(name);
    setAddress(address);
    setCapacity(capacity);
  }

  // Getters //

  @Override
  public UUID getId() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getAddress() {
    return address;
  }

  @Override
  public int getCapacity() {
    return capacity;
  }

  // Setters //
  
  /**
   * Sets the address of the venue.
   *
   * @param name the new name; must not be {@code null} or blank.
   *
   * @throws IllegalArgumentException if the name is invalid.
   */
  private void setName(String name) {
    if (name == null || name.isBlank()) {
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
    if (address == null || address.isBlank()) {
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
    if (capacity < 1) {
      throw new IllegalArgumentException("Venue capacity must be greater than zero.");
    }
    this.capacity = capacity;
  }

  // Method overrides //
  
  /** Returns a string representation for debugging purposes. */
  @Override
  public String toString() {
    return String.format("Venue[id=%s, name='%s', address='%s', capacity=%d]", id, name, address,
        capacity);
  }
}
