package york.studentevents.venues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import york.studentevents.exceptions.VenueNotFoundException;
import york.studentevents.repository.inmemory.InMemoryVenueRepository;

/**
 * Tests {@link VenueService} against a real {@link InMemoryVenueRepository}.
 *
 * <p>The service delegates persistence to the repository and delegates field validation to the
 * {@link Venue} entity's setters, so these tests exercise both the service's own behaviour (lookup,
 * error translation, capacity filtering, persistence) and its correct wiring into the entity's
 * validation rules.
 */
class VenueServiceTest {

  private InMemoryVenueRepository repository;
  private VenueService service;

  @BeforeEach
  void setUp() {
    repository = new InMemoryVenueRepository();
    service = new VenueService(repository);
  }

  // --- constructor ---

  @Test
  void constructor_withNullRepository_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> new VenueService(null));
  }

  // --- getVenue ---

  @Test
  void getVenue_whenVenueExists_returnsVenue() {
    IVenue venue = savedVenue();

    assertEquals(venue, service.getVenue(venue.getId()));
  }

  @Test
  void getVenue_whenVenueDoesNotExist_throwsVenueNotFoundException() {
    assertThrows(VenueNotFoundException.class, () -> service.getVenue(UUID.randomUUID()));
  }

  // --- createVenue ---

  @Test
  void createVenue_withCapacity_createsAndPersistsVenue() {
    IVenue venue = service.createVenue("Central Hall", "Campus West", 1200);

    assertEquals("Central Hall", venue.getName());
    assertEquals("Campus West", venue.getAddress());
    assertEquals(1200, venue.getCapacity());
    assertEquals(venue, service.getVenue(venue.getId()));
  }

  @Test
  void createVenue_withNullCapacity_createsUncappedVenue() {
    IVenue venue = service.createVenue("The Courtyard", "Campus West", null);

    assertNull(venue.getCapacity());
    assertEquals(venue, service.getVenue(venue.getId()));
  }

  @Test
  void createVenue_withNullName_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> service.createVenue(null, "Campus West", 100));
  }

  @Test
  void createVenue_withBlankName_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> service.createVenue("   ", "Campus West", 100));
  }

  @Test
  void createVenue_withNullAddress_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> service.createVenue("Central Hall", null, 100));
  }

  @Test
  void createVenue_withBlankAddress_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> service.createVenue("Central Hall", "   ", 100));
  }

  @Test
  void createVenue_withCapacityBelowOne_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> service.createVenue("Central Hall", "Campus West", 0));
  }

  @Test
  void createVenue_whenValidationFails_doesNotPersist() {
    assertThrows(IllegalArgumentException.class,
        () -> service.createVenue(null, "Campus West", 100));

    assertTrue(service.getAllVenues().isEmpty());
  }

  // --- updateVenueTitle ---

  @Test
  void updateVenueTitle_updatesAndPersistsName() {
    IVenue venue = savedVenue();

    service.updateVenueTitle(venue.getId(), "New Name");

    assertEquals("New Name", service.getVenue(venue.getId()).getName());
  }

  @Test
  void updateVenueTitle_withNullName_throwsIllegalArgumentException() {
    IVenue venue = savedVenue();

    assertThrows(IllegalArgumentException.class,
        () -> service.updateVenueTitle(venue.getId(), null));
  }

  @Test
  void updateVenueTitle_withBlankName_throwsIllegalArgumentException() {
    IVenue venue = savedVenue();

    assertThrows(IllegalArgumentException.class,
        () -> service.updateVenueTitle(venue.getId(), "   "));
  }

  @Test
  void updateVenueTitle_whenVenueDoesNotExist_throwsVenueNotFoundException() {
    assertThrows(VenueNotFoundException.class,
        () -> service.updateVenueTitle(UUID.randomUUID(), "New Name"));
  }

  // --- updateVenueAddress ---

  @Test
  void updateVenueAddress_updatesAndPersistsAddress() {
    IVenue venue = savedVenue();

    service.updateVenueAddress(venue.getId(), "Campus East");

    assertEquals("Campus East", service.getVenue(venue.getId()).getAddress());
  }

  @Test
  void updateVenueAddress_withNullAddress_throwsIllegalArgumentException() {
    IVenue venue = savedVenue();

    assertThrows(IllegalArgumentException.class,
        () -> service.updateVenueAddress(venue.getId(), null));
  }

  @Test
  void updateVenueAddress_withBlankAddress_throwsIllegalArgumentException() {
    IVenue venue = savedVenue();

    assertThrows(IllegalArgumentException.class,
        () -> service.updateVenueAddress(venue.getId(), "   "));
  }

  @Test
  void updateVenueAddress_whenVenueDoesNotExist_throwsVenueNotFoundException() {
    assertThrows(VenueNotFoundException.class,
        () -> service.updateVenueAddress(UUID.randomUUID(), "Campus East"));
  }

  // --- updateVenueCapacity ---

  @Test
  void updateVenueCapacity_updatesAndPersistsCapacity() {
    IVenue venue = savedVenue();

    service.updateVenueCapacity(venue.getId(), 50);

    assertEquals(50, service.getVenue(venue.getId()).getCapacity());
  }

  @Test
  void updateVenueCapacity_withNull_removesCapacity() {
    IVenue venue = savedVenueWithCapacity(50);

    service.updateVenueCapacity(venue.getId(), null);

    assertNull(service.getVenue(venue.getId()).getCapacity());
  }

  @Test
  void updateVenueCapacity_belowOne_throwsIllegalArgumentException() {
    IVenue venue = savedVenue();

    assertThrows(IllegalArgumentException.class,
        () -> service.updateVenueCapacity(venue.getId(), 0));
  }

  @Test
  void updateVenueCapacity_whenVenueDoesNotExist_throwsVenueNotFoundException() {
    assertThrows(VenueNotFoundException.class,
        () -> service.updateVenueCapacity(UUID.randomUUID(), 50));
  }

  // --- getAllVenues ---

  @Test
  void getAllVenues_whenEmpty_returnsEmptyList() {
    assertTrue(service.getAllVenues().isEmpty());
  }

  @Test
  void getAllVenues_returnsAllSavedVenues() {
    IVenue first = savedVenue();
    IVenue second = savedVenue();

    List<IVenue> venues = service.getAllVenues();

    assertEquals(2, venues.size());
    assertTrue(venues.contains(first));
    assertTrue(venues.contains(second));
  }

  // --- getVenuesByCapacity ---

  @Test
  void getVenuesByCapacity_withBothBoundsNull_returnsAllVenues() {
    IVenue capped = savedVenueWithCapacity(100);
    IVenue uncapped = savedVenueWithCapacity(null);

    List<IVenue> results = service.getVenuesByCapacity(null, null);

    assertEquals(2, results.size());
    assertTrue(results.contains(capped));
    assertTrue(results.contains(uncapped));
  }

  @Test
  void getVenuesByCapacity_withMinBoundOnly_returnsVenuesAtOrAboveIt() {
    savedVenueWithCapacity(50); // below bound
    IVenue atOrAbove = savedVenueWithCapacity(200);

    List<IVenue> results = service.getVenuesByCapacity(100, null);

    assertEquals(List.of(atOrAbove), results);
  }

  @Test
  void getVenuesByCapacity_withMinBoundOnly_includesUncappedVenues() {
    IVenue uncapped = savedVenueWithCapacity(null);

    // An uncapped (unlimited) venue always satisfies a lower bound.
    List<IVenue> results = service.getVenuesByCapacity(100, null);

    assertTrue(results.contains(uncapped));
  }

  @Test
  void getVenuesByCapacity_withMaxBoundOnly_returnsVenuesAtOrBelowIt() {
    IVenue atOrBelow = savedVenueWithCapacity(50);
    savedVenueWithCapacity(200); // above bound

    List<IVenue> results = service.getVenuesByCapacity(null, 100);

    assertEquals(List.of(atOrBelow), results);
  }

  @Test
  void getVenuesByCapacity_withMaxBoundOnly_excludesUncappedVenues() {
    savedVenueWithCapacity(null);

    // An uncapped (unlimited) venue exceeds any finite upper bound.
    assertTrue(service.getVenuesByCapacity(null, 100).isEmpty());
  }

  @Test
  void getVenuesByCapacity_withBothBounds_returnsOnlyVenuesWithinTheRange() {
    IVenue within = savedVenueWithCapacity(150);
    savedVenueWithCapacity(50); // below range
    savedVenueWithCapacity(500); // above range

    List<IVenue> results = service.getVenuesByCapacity(100, 200);

    assertEquals(List.of(within), results);
  }

  @Test
  void getVenuesByCapacity_minBoundIsInclusive() {
    IVenue onBoundary = savedVenueWithCapacity(100);

    List<IVenue> results = service.getVenuesByCapacity(100, null);

    assertEquals(List.of(onBoundary), results);
  }

  @Test
  void getVenuesByCapacity_maxBoundIsInclusive() {
    IVenue onBoundary = savedVenueWithCapacity(100);

    List<IVenue> results = service.getVenuesByCapacity(null, 100);

    assertEquals(List.of(onBoundary), results);
  }

  @Test
  void getVenuesByCapacity_whenNoVenueMatches_returnsEmptyList() {
    savedVenueWithCapacity(50);
    savedVenueWithCapacity(60);

    assertTrue(service.getVenuesByCapacity(100, 200).isEmpty());
  }

  @Test
  void getVenuesByCapacity_whenNoVenuesSaved_returnsEmptyList() {
    assertTrue(service.getVenuesByCapacity(100, 200).isEmpty());
  }

  // --- deleteVenue ---

  @Test
  void deleteVenue_removesVenue() {
    IVenue venue = savedVenue();

    service.deleteVenue(venue.getId());

    assertFalse(service.getAllVenues().contains(venue));
    assertThrows(VenueNotFoundException.class, () -> service.getVenue(venue.getId()));
  }

  @Test
  void deleteVenue_whenVenueDoesNotExist_throwsVenueNotFoundException() {
    assertThrows(VenueNotFoundException.class,
        () -> service.deleteVenue(UUID.randomUUID()));
  }

  // --- helpers ---

  private IVenue savedVenue() {
    IVenue venue = new Venue("The Courtyard", "Campus West");
    repository.save(venue);
    return venue;
  }

  private IVenue savedVenueWithCapacity(Integer capacity) {
    IVenue venue =
        capacity == null
            ? new Venue("The Courtyard", "Campus West")
            : new Venue("The Courtyard", "Campus West", capacity);
    repository.save(venue);
    return venue;
  }
}
