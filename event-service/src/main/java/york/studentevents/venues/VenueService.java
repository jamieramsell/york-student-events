package york.studentevents.venues;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import york.studentevents.exceptions.VenueNotFoundException;

/**
 * Application service exposing venue-related business operations.
 *
 * <p>This service forms the {@code Service} layer of the stack, sitting between the
 * {@link VenueController} and the {@link IVenueRepository}. Persistence is delegated to the
 * injected repository; the service holds no state of its own.
 *
 * @see IVenueRepository
 * @see IVenue
 */
public class VenueService {

  private IVenueRepository repository;

  /**
   * Constructs a {@code VenueService} backed by the given repository.
   *
   * @param repositoryInjection the repository used to store and retrieve venues; must not be
   *     {@code null}
   */
  public VenueService(IVenueRepository repositoryInjection) {
    if (repositoryInjection == null) {
      throw new IllegalArgumentException("repositoryInjection must not be null");
    }
    this.repository = repositoryInjection;
  }

  /**
   * Retrieves a Venue from the injected VenueRepository.
   *
   * @param venueId the venue's ID
   * @return the {@code IVenue} entity; never null
   * @throws VenueNotFoundException if the given Venue does not exist
   */
  public IVenue getVenue(UUID venueId) {
    Optional<IVenue> optionalVenue = repository.findByID(venueId);
    if (optionalVenue.isEmpty()) {
      throw new VenueNotFoundException();
    }
    return optionalVenue.get();
  }

  /**
   * Creates and stores a record of a Venue.
   *
   * @param name the venue's name; must not be null.
   * @param address the venue's address; must not be null.
   * @param capacity the (optional) maximum capacity of the venue.
   * @return a copy of the created {@code IVenue} entity.
   *
   * @throws IllegalArgumentException if any non-nullable argument is null, or if a capacity is
   *     given which is less than 1.
   */
  public IVenue createVenue(String name, String address, Integer capacity) {
    IVenue venue;

    if (capacity == null) {
      venue = new Venue(name, address);
    } else {
      venue = new Venue(name, address, capacity);
    }

    repository.save(venue);
    return venue;
  }

  /**
   * Retrieves a Venue record, and updates its name field.
   *
   * @param id The ID of the target Venue.
   * @param name The new Venue name; must not be null or empty.
   * @return A copy of the updated Venue record.
   *
   * @throws VenueNotFoundException if an Venue with the given ID could not be found.
   * @throws IllegalArgumentException if name is null or empty.
   */
  public IVenue updateVenueTitle(UUID id, String name) {
    IVenue venue = getVenue(id);
    venue.setName(name);
    repository.save(venue);
    return venue;
  }

  /**
   * Retrieves an Venue record, and updates its address field.
   *
   * @param id The ID of the target Venue.
   * @param address The new address of the Venue.
   * @return A copy of the updated Venue record.
   *
   * @throws VenueNotFoundException if a Venue with the given ID could not be found.
   */
  public IVenue updateVenueAddress(UUID id, String address) {
    IVenue venue = getVenue(id);
    venue.setAddress(address);
    repository.save(venue);
    return venue;
  }

  /**
   * Retrieves a Venue record, and updates its capacity field.
   *
   * <p>Passing a {@code null} capacity will remove its prescribed maximum capacity.
   *
   * @param id The ID of the target Venue.
   * @param capacity The (optional) new Venue capacity.
   * @return A copy of the updated Venue record.
   *
   * @throws VenueNotFoundException if an venue with the given ID could not be found.
   */
  public IVenue updateVenueCapacity(UUID id, Integer capacity) {
    IVenue venue = getVenue(id);
    venue.setCapacity(capacity);
    repository.save(venue);
    return venue;
  }

  /**
   * Retrieves every Venue currently held by the backing repository.
   *
   * @return a {@link List} of all Venues; never {@code null}, but may be empty if no Venues have
   *     been saved
   */
  public List<IVenue> getAllVenues() {
    return repository.findAll();
  }

  /**
   * Retrieves all Venues which have a capacity within the given bounds. The two boundaries are
   * inclusive, so a Venue which has a capacity identical to the {@code minimum} boundary for
   * example will be retrieved.
   *
   * <p>The capacity boundaries are nullable, meaning you can have a minimum bound without a maximum
   * one, and vice versa.
   *
   * @param minimum The (optional) minimum venue capacity
   * @param maximum The (optional) maximum venue capacity
   * @return All venues which have a capacity between the two specified bounds, inclusive.
   */
  public List<IVenue> getVenuesByCapacity(Integer minimum, Integer maximum) {
    return getAllVenues()
        .stream()
        .filter(venue -> venueCapacityWithin(venue, minimum, maximum))
        .toList();
  }

  /**
   * Helper method which checks whether the capacity of a given Venue falls within two given values.
   * This check is inclusive of either boundary.
   *
   * <p>{@code min} and {@code max} are nullable, which can be used if wanting to check whether a
   *     Venue has a minimum or a maximum capacity, without enforcing a bound on the other side.
   *
   * @param venue The Venue to check
   * @param min The (optional) minimum venue capacity
   * @param max The (optional) maximum venue capacity
   * @return Whether or not the Venue's capacity falls within the two specified values
   */
  private boolean venueCapacityWithin(IVenue venue, Integer min, Integer max) {
    if (
        min != null
        && venue.getCapacity() != null
        && venue.getCapacity() < min
    ) {
      return false;
    }

    if (max != null) {
      if (venue.getCapacity() == null) {
        // If the venue has an unlimited capacity, then it does indeed exceed the max, as its
        // capacity is infinite
        return false;
      } else if (venue.getCapacity() > max) {
        return false;
      }
    }

    // If this point is reached, then the event does lie within the two points (inclusively)
    return true;
  }

  /**
   * Deletes a Venue from the injected VenueRepository.
   *
   * @param venueId the Venue's ID
   * @throws VenueNotFoundException if the given Venue does not exist
   */
  public void deleteVenue(UUID venueId) {
    try {
      repository.delete(venueId);
    } catch (NoSuchElementException e) {
      throw new VenueNotFoundException();
    }
  }

}
