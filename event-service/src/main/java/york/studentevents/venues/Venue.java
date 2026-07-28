package york.studentevents.venues;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;

/** Represents a venue at which events can be held, identified by name, address, and capacity. */
@Entity
public class Venue implements IVenue {

  @Id
  private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
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

  /**
   * Creates a {@code Venue} without a maximum attendee capacity.
   *
   * @param name the name of the venue.
   * @param address the address of the venue.
   * 
   * @throws IllegalArgumentException if any of the parameters are {@code null}, blank, or empty
   */
  public Venue(String name, String address) {
    this(UUID.randomUUID(), name, address);
  }

  /**
   * Creates a {@code Venue} with a specific ID value, without a maximum attendee capacity.
   *
   * @param id the venue's ID.
   * @param name the name of the venue.
   * @param address the address of the venue.
   * 
   * @throws IllegalArgumentException if any of the parameters are {@code null}, blank, or empty.
   */
  protected Venue(UUID id, String name, String address) {
    if (id == null) {
      throw new IllegalArgumentException("Venue ID cannot be null");
    }
    this.id = id;
    setName(name);
    setAddress(address);
    setCapacity(null);
  }

  /** No-args constructor for JPA use only. */
  protected Venue() {}

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
  public Integer getCapacity() {
    return capacity;
  }

  // Setters //
  
  @Override
  public void setName(String name) {
    if (name == null || name.isBlank() || name.isEmpty()) {
      throw new IllegalArgumentException("Venue name cannot be null, blank, or empty.");
    }
    this.name = name;
  }

  @Override
  public void setAddress(String address) {
    if (address == null || address.isBlank() || address.isEmpty()) {
      throw new IllegalArgumentException("Venue address cannot be null, blank, or empty.");
    }
    this.address = address;
  }

  @Override
  public void setCapacity(Integer capacity) {
    if (capacity != null && capacity < 1) {
      throw new IllegalArgumentException("Venue capacity must be greater than zero, or null.");
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
