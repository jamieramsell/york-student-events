package york.studentevents.venues;

import java.util.HashSet;
import java.util.Set;

import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
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

    assertEquals(0, service.getAllVenues(0, 100).getTotalElements());
  }

  // --- updateVenueName ---

  @Test
  void updateVenueName_updatesAndPersistsName() {
    IVenue venue = savedVenue();

    service.updateVenueName(venue.getId(), "New Name");

    assertEquals("New Name", service.getVenue(venue.getId()).getName());
  }

  @Test
  void updateVenueName_withNullName_throwsIllegalArgumentException() {
    IVenue venue = savedVenue();

    assertThrows(IllegalArgumentException.class,
        () -> service.updateVenueName(venue.getId(), null));
  }

  @Test
  void updateVenueName_withBlankName_throwsIllegalArgumentException() {
    IVenue venue = savedVenue();

    assertThrows(IllegalArgumentException.class,
        () -> service.updateVenueName(venue.getId(), "   "));
  }

  @Test
  void updateVenueName_whenVenueDoesNotExist_throwsVenueNotFoundException() {
    assertThrows(VenueNotFoundException.class,
        () -> service.updateVenueName(UUID.randomUUID(), "New Name"));
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
  void getAllVenues_whenEmpty_returnsEmptyPage() {
    assertEquals(0, service.getAllVenues(0, 100).getTotalElements());
  }

  @Test
  void getAllVenues_returnsAllSavedVenues() {
    IVenue first = savedVenue();
    IVenue second = savedVenue();

    Page<IVenue> venues = service.getAllVenues(0, 100);
    Set<IVenue> results = new HashSet<>(venues.getContent());

    assertEquals(2, venues.getTotalElements());
    assertTrue(results.contains(first));
    assertTrue(results.contains(second));
  }

  // --- getVenuesByCapacity ---

  @Test
  void getAllVenuesByCapacity_withBothBoundsNull_returnsAllVenues() {
    IVenue capped = savedVenueWithCapacity(100);
    IVenue uncapped = savedVenueWithCapacity(null);

    Page<IVenue> venues = service.getAllVenuesByCapacity(null, null, 0, 100);
    Set<IVenue> results = new HashSet<>(venues.getContent());

    assertEquals(2, venues.getTotalElements());
    assertTrue(results.contains(capped));
    assertTrue(results.contains(uncapped));
  }

  @Test
  void getAllVenuesByCapacity_withMinBoundOnly_returnsAllVenuesAtOrAboveIt() {
    savedVenueWithCapacity(50); // below bound
    IVenue atOrAbove = savedVenueWithCapacity(200);

    Page<IVenue> venues = service.getAllVenuesByCapacity(100, null, 0, 100);

    assertEquals(List.of(atOrAbove), venues.getContent());
  }

  @Test
  void getAllVenuesByCapacity_withMinBoundOnly_includesUncappedVenues() {
    IVenue uncapped = savedVenueWithCapacity(null);

    // An uncapped (unlimited) venue always satisfies a lower bound.
    Set<IVenue> results = new HashSet<>(
        service.getAllVenuesByCapacity(
        100, null, 0, 100
        ).getContent()
    );

    assertTrue(results.contains(uncapped));
  }

  @Test
  void getAllVenuesByCapacity_withMaxBoundOnly_returnsAllVenuesAtOrBelowIt() {
    IVenue atOrBelow = savedVenueWithCapacity(50);
    savedVenueWithCapacity(200); // above bound

    List<IVenue> results = service.getAllVenuesByCapacity(
        null, 100, 0, 100
    ).getContent();

    assertEquals(List.of(atOrBelow), results);
  }

  @Test
  void getAllVenuesByCapacity_withMaxBoundOnly_excludesUncappedVenues() {
    savedVenueWithCapacity(null);

    // An uncapped (unlimited) venue exceeds any finite upper bound.
    assertEquals(0, service.getAllVenuesByCapacity(
        null, 100, 0, 100
    ).getTotalElements());
  }

  @Test
  void getAllVenuesByCapacity_withBothBounds_returnsOnlyAllVenuesWithinTheRange() {
    IVenue within = savedVenueWithCapacity(150);
    savedVenueWithCapacity(50); // below range
    savedVenueWithCapacity(500); // above range

    List<IVenue> results = service.getAllVenuesByCapacity(
        100, 200, 0, 100
    ).getContent();

    assertEquals(List.of(within), results);
  }

  @Test
  void getAllVenuesByCapacity_minBoundIsInclusive() {
    IVenue onBoundary = savedVenueWithCapacity(100);

    List<IVenue> results = service.getAllVenuesByCapacity(
        100, null, 0, 100
    ).getContent();

    assertEquals(List.of(onBoundary), results);
  }

  @Test
  void getAllVenuesByCapacity_maxBoundIsInclusive() {
    IVenue onBoundary = savedVenueWithCapacity(100);

    List<IVenue> results = service.getAllVenuesByCapacity(
        null, 100, 0, 100
    ).getContent();

    assertEquals(List.of(onBoundary), results);
  }

  @Test
  void getAllVenuesByCapacity_whenNoVenueMatches_returnsEmptyPage() {
    savedVenueWithCapacity(50);
    savedVenueWithCapacity(60);

    assertEquals(0,
        service.getAllVenuesByCapacity(
          100, 200, 0, 100
        ).getTotalElements()
    );
  }

  @Test
  void getAllVenuesByCapacity_whenNoVenuesSaved_returnsEmptyPage() {
    assertEquals(0,
        service.getAllVenuesByCapacity(
            100, 200, 0, 100
        ).getTotalElements()
    );
  }

  // --- deleteVenue ---

  @Test
  void deleteVenue_removesVenue() {
    IVenue venue = savedVenue();

    service.deleteVenue(venue.getId());

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
