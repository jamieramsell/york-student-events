package york.studentevents.venues;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.lang.NonNull;

/** Represents a venue at which events can be held, identified by name, address, and capacity. */
@Entity
public class Venue implements IVenue {

  @Id
  private UUID id;

  @Column(nullable = false)
  @NotBlank
  private String name;

  @Column(nullable = false)
  @NotBlank
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
  public Venue(@NotBlank String name, @NotBlank String address, int capacity) {
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
  public Venue(@NonNull UUID id, @NotBlank String name, @NotBlank String address, int capacity) {
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
  public Venue(@NotBlank String name, @NotBlank String address) {
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
  public Venue(@NonNull UUID id, @NotBlank String name, @NotBlank String address) {
    this.id = id;
    setName(name);
    setAddress(address);
    setCapacity(null);
  }

  /** No-args constructor for JPA use only. */
  protected Venue() {}

  // Getters //
  @Override
  @NonNull
  public UUID getId() {
    return id;
  }

  @Override
  @NotNull
  public String getName() {
    return name;
  }

  @Override
  @NotBlank
  public String getAddress() {
    return address;
  }

  @Override
  public Integer getCapacity() {
    return capacity;
  }

  // Setters //
  
  @Override
  public void setName(@NotBlank String name) {
    this.name = name;
  }

  @Override
  public void setAddress(@NotBlank String address) {
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
