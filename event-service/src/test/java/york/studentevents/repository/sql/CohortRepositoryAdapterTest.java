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
import york.studentevents.cohorts.Cohort;
import york.studentevents.cohorts.ICohort;
import york.studentevents.cohorts.ICohortRepository;

/**
 * Integration tests for {@link CohortRepositoryAdapter}, exercising it against a real Spring Data
 * JPA layer backed by an in-memory H2 database via
 * {@link org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest}.
 *
 * <p>Each test constructs the adapter around the injected {@link JpaCohortRepository} proxy, so the
 *     assertions cover both the CRUD delegation and the adapter's own translation behaviour: the
 *     null-guards on {@code save}, {@code delete} and {@code findByID}, the mapping of a missing
 *     row to {@link java.util.Optional#empty()}, the {@link java.util.NoSuchElementException}
 *     raised when deleting a non-existent event, and the overwrite-on-save semantics of an existing
 *     ID.
 *
 * @see CohortRepositoryAdapter
 * @see JpaCohortRepository
 */
@DataJpaTest 
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class CohortRepositoryAdapterTest {
  
  @Autowired
  private JpaCohortRepository jpa;

  private ICohortRepository cohortRepository;

  @BeforeEach
  void setUp() {
    cohortRepository = new CohortRepositoryAdapter(jpa);
  }

  @Test
  void save_RejectsNullCohort() {
    ICohort nullCohort = null;
    assertThrows(IllegalArgumentException.class, () -> cohortRepository.save(nullCohort));
  }

  @Test
  void save_PersistsToJpa() {
    ICohort cohort = new Cohort("Name", "Department", 2026, 1);
    cohortRepository.save(cohort);
    assertTrue(jpa.existsById(cohort.getId()));
  }

  @Test 
  void save_WithExistingId_Overwrites() {
    ICohort cohort = new Cohort("Name", "Computer Science", 2026, 1);
    cohortRepository.save(cohort);

    cohort.setDepartment("Mathematics"); // Cohort keeps its original ID but has a new department
    cohortRepository.save(cohort);
    
    // Use JPA here to avoid relying on a separate adapter method
    assertEquals(1, jpa.findAll().size()); 

    // Assert that the saved email address has been overwritten
    assertEquals(cohort.getDepartment(), jpa.findById(cohort.getId()).get().getDepartment());
  }

  @Test
  void delete_RejectsNullId() {
    UUID nullId = null;
    assertThrows(IllegalArgumentException.class, () -> cohortRepository.delete(nullId));
  }

  @Test
  void delete_RejectsNonexistentCohort() {
    UUID fakeId = UUID.randomUUID();
    assertThrows(NoSuchElementException.class, () -> cohortRepository.delete(fakeId));
  }

  @Test 
  void delete_RemovesCohortFromJpa() {
    Cohort cohort = new Cohort("Name", "Dept", 2026, 1);
    jpa.saveAndFlush(cohort); // Use JPA here to avoid relying on a separate adapter method

    cohortRepository.delete(cohort.getId());
    assertFalse(jpa.existsById(cohort.getId()));
  }

  @Test
  void findById_RejectsNullId() {
    UUID nullId = null;
    assertThrows(IllegalArgumentException.class, () -> cohortRepository.findByID(nullId));
  }

  @Test
  void findById_ReturnsEmptyOptional_OnNonexistentCohort() {
    UUID fakeId = UUID.randomUUID();
    Optional<ICohort> emptyOptional = Optional.empty();
    assertEquals(emptyOptional, cohortRepository.findByID(fakeId));
  }

  @Test
  void findById_RetrievesCohortFromJpa() {
    Cohort cohort = new Cohort("Name", "Dept", 2026, 1);
    jpa.saveAndFlush(cohort); // Use JPA here to avoid relying on a separate adapter method

    ICohort retrievedCohort = cohortRepository.findByID(cohort.getId()).get();
    assertEquals(cohort.getId(), retrievedCohort.getId());
    assertEquals(cohort.getAcademicYear(), retrievedCohort.getAcademicYear());
    assertEquals(cohort.getDepartment(), retrievedCohort.getDepartment());
    assertEquals(cohort.getName(), retrievedCohort.getName());
    assertEquals(cohort.getYearGroup(), retrievedCohort.getYearGroup());
  }

  @Test 
  void findAll_RetrievesAllCohorts() {
    List<Cohort> cohortList = new ArrayList<>();
    cohortList.add(new Cohort("Name1", "Dept", 2026, 1));
    cohortList.add(new Cohort("Name2", "Dept", 2026, 1));
    cohortList.add(new Cohort("Name3", "Dept", 2026, 1));
    jpa.saveAllAndFlush(cohortList); // Use JPA here to avoid relying on a separate adapter method

    List<ICohort> savedCohorts = cohortRepository.findAll();
    assertEquals(3, savedCohorts.size());
  }

  @Test 
  void findAll_ReturnsEmptyList_ForEmptyRepository() {
    assertTrue(cohortRepository.findAll().isEmpty());
  }
}
