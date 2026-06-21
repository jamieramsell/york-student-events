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
   * @param name the name of the venue; must not be {@code null} or blank
   * @param address the address of the venue; must not be {@code null} or blank
   * @param capacity the maximum number of attendees the venue can hold; must be greater than 0
   */
  public Venue(String name, String address, int capacity) {
    // Validation
    if (name == null || name.isBlank() || name.isEmpty()) {
      throw new IllegalArgumentException("Venue name cannot be null, blank, or empty.");
    } else if (address == null || address.isBlank() || address.isEmpty()) {
      throw new IllegalArgumentException("Venue address cannot be null, blank, or empty.");
    } else if (capacity <= 0) {
      throw new IllegalArgumentException("Venue capacity must be greater than 0.");
    }

    this.id = UUID.randomUUID();
    this.name = name;
    this.address = address;
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
