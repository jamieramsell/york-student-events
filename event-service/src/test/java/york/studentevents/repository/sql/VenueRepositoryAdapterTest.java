package york.studentevents.repository.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import york.studentevents.venues.IVenue;
import york.studentevents.venues.IVenueRepository;
import york.studentevents.venues.Venue;

/**
 * Integration tests for {@link VenueRepositoryAdapter}, exercising it against a real Spring Data
 * JPA layer backed by an in-memory H2 database via
 * {@link org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest}.
 *
 * <p>Each test constructs the adapter around the injected {@link JpaVenueRepository} proxy, so the
 *     assertions cover both the CRUD delegation and the adapter's own translation behaviour: the
 *     null-guards on {@code save}, {@code delete} and {@code findByID}, the mapping of a missing
 *     row to {@link java.util.Optional#empty()}, the {@link java.util.NoSuchElementException}
 *     raised when deleting a non-existent event, and the overwrite-on-save semantics of an existing
 *     ID.
 *
 * @see VenueRepositoryAdapter
 * @see JpaVenueRepository
 */
@DataJpaTest 
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class VenueRepositoryAdapterTest {
  
  @Autowired
  private JpaVenueRepository jpa;

  private IVenueRepository venueRepository;

  @BeforeEach
  void setUp() {
    venueRepository = new VenueRepositoryAdapter(jpa);
  }

  @Test
  void save_RejectsNullVenue() {
    IVenue nullVenue = null;
    assertThrows(IllegalArgumentException.class, () -> venueRepository.save(nullVenue));
  }

  @Test
  void save_PersistsToJpa() {
    IVenue venue = new Venue("Test Title", "7 Barber Close");
    venueRepository.save(venue);
    assertTrue(jpa.existsById(venue.getId()));
  }

  @Test 
  void save_WithExistingId_Overwrites() {
    IVenue venue = new Venue("Test Title 1", "7 Barber Close");
    venueRepository.save(venue);

    venue.setAddress("1 Oaktree Way"); // Venue keeps its original ID but has a new address record
    venueRepository.save(venue);
    
    // Use JPA here to avoid relying on a separate adapter method
    assertEquals(1, jpa.findAll().size()); 

    // Assert that the saved address has been overwritten
    assertEquals(venue.getAddress(), jpa.findById(venue.getId()).get().getAddress());
  }

  @Test
  void delete_RejectsNullId() {
    UUID nullId = null;
    assertThrows(IllegalArgumentException.class, () -> venueRepository.delete(nullId));
  }

  @Test
  void delete_RejectsNonexistentVenue() {
    UUID fakeId = UUID.randomUUID();
    assertThrows(NoSuchElementException.class, () -> venueRepository.delete(fakeId));
  }

  @Test 
  void delete_RemovesVenueFromJpa() {
    Venue venue = new Venue("Test Title", "7 Barber Close");
    jpa.saveAndFlush(venue); // Use JPA here to avoid relying on a separate adapter method

    venueRepository.delete(venue.getId());
    assertFalse(jpa.existsById(venue.getId()));
  }

  @Test
  void findById_RejectsNullId() {
    UUID nullId = null;
    assertThrows(IllegalArgumentException.class, () -> venueRepository.findByID(nullId));
  }

  @Test
  void findById_ReturnsEmptyOptional_OnNonexistentVenue() {
    UUID fakeId = UUID.randomUUID();
    Optional<IVenue> emptyOptional = Optional.empty();
    assertEquals(emptyOptional, venueRepository.findByID(fakeId));
  }

  @Test
  void findById_RetrievesVenueFromJpa() {
    Venue venue = new Venue("Test Title", "43 Hempland Lane");
    jpa.saveAndFlush(venue); // Use JPA here to avoid relying on a separate adapter method

    IVenue retrievedVenue = venueRepository.findByID(venue.getId()).get();
    assertEquals(venue.getId(), retrievedVenue.getId());
    assertEquals(venue.getAddress(), retrievedVenue.getAddress());
    assertEquals(venue.getName(), retrievedVenue.getName());
  }

  @Test 
  void findAll_RetrievesAllVenues() {
    List<Venue> venueList = new ArrayList<>();
    venueList.add(new Venue("Title 1", "43 Hempland Lane"));
    venueList.add(new Venue("Title 1", "7 Barber Close"));
    venueList.add(new Venue("Title 1", "1 Oaktree Way"));
    jpa.saveAllAndFlush(venueList); // Use JPA here to avoid relying on a separate adapter method

    List<IVenue> savedVenues = venueRepository.findAll();
    assertEquals(3, savedVenues.size());
  }

  @Test 
  void findAll_ReturnsEmptyList_ForEmptyRepository() {
    assertTrue(venueRepository.findAll().isEmpty());
  }
}
