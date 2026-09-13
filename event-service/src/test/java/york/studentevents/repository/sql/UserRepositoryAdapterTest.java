package york.studentevents.repository.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
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
import york.studentevents.events.Event;
import york.studentevents.events.EventCategory;
import york.studentevents.events.IEvent;
import york.studentevents.events.IEventRepository;
import york.studentevents.users.IUser;
import york.studentevents.users.IUserRepository;
import york.studentevents.users.Student;
import york.studentevents.users.User;

/**
 * Integration tests for {@link UserRepositoryAdapter}, exercising it against a real Spring Data
 * JPA layer backed by an in-memory H2 database via
 * {@link org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest}.
 *
 * <p>Each test constructs the adapter around the injected {@link JpaUserRepository} proxy, so the
 *     assertions cover both the CRUD delegation and the adapter's own translation behaviour: the
 *     null-guards on {@code save}, {@code delete} and {@code findByID}, the mapping of a missing
 *     row to {@link java.util.Optional#empty()}, the {@link java.util.NoSuchElementException}
 *     raised when deleting a non-existent event, and the overwrite-on-save semantics of an existing
 *     ID.
 *
 * @see UserRepositoryAdapter
 * @see JpaUserRepository
 */
@DataJpaTest 
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class UserRepositoryAdapterTest {
  
  @Autowired
  private JpaUserRepository jpa;

  private IUserRepository userRepository;

  @BeforeEach
  void setUp() {
    userRepository = new UserRepositoryAdapter(jpa);
  }

  @Test
  void save_RejectsNullUser() {
    IUser nullUser = null;
    assertThrows(IllegalArgumentException.class, () -> userRepository.save(nullUser));
  }

  @Test
  void save_PersistsToJpa() {
    IUser user = new Student("username", "email@provider.com", "hash", new HashSet<>());
    userRepository.save(user);
    assertTrue(jpa.existsById(user.getId()));
  }

  @Test 
  void save_WithExistingId_Overwrites() {
    IUser user = new Student("username", "email@provider.com", "hash", new HashSet<>());
    userRepository.save(user);

    user.setEmail("email2@provider2.co.uk"); // User keeps its original ID here but has a new email
    userRepository.save(user);
    
    // Use JPA here to avoid relying on a seperate adapter method
    assertEquals(1, jpa.findAll().size()); 

    // Assert that the saved email address has been overwritten
    assertEquals(user.getEmail(), jpa.findById(user.getId()).get().getEmail());
  }

  @Test
  void delete_RejectsNullId() {
    UUID nullId = null;
    assertThrows(IllegalArgumentException.class, () -> userRepository.delete(nullId));
  }

  @Test
  void delete_RejectsNonexistentUser() {
    UUID fakeId = UUID.randomUUID();
    assertThrows(NoSuchElementException.class, () -> userRepository.delete(fakeId));
  }

  @Test 
  void delete_RemovesUserFromJpa() {
    User user = new Student("username", "email@provider.com", "hash", new HashSet<>());
    jpa.saveAndFlush(user); // Use JPA here to avoid relying on a seperate adapter method

    userRepository.delete(user.getId());
    assertFalse(jpa.existsById(user.getId()));
  }

  @Test
  void findById_RejectsNullId() {
    UUID nullId = null;
    assertThrows(IllegalArgumentException.class, () -> userRepository.findByID(nullId));
  }

  @Test
  void findById_ReturnsEmptyOptional_OnNonexistentUser() {
    UUID fakeId = UUID.randomUUID();
    Optional<IUser> emptyOptional = Optional.empty();
    assertEquals(emptyOptional, userRepository.findByID(fakeId));
  }

  @Test
  void findById_RetrievesUserFromJpa() {
    User user = new Student("username", "email@provider.com", "hash", new HashSet<>());
    jpa.saveAndFlush(user); // Use JPA here to avoid relying on a seperate adapter method

    IUser retrievedUser = userRepository.findByID(user.getId()).get();
    assertEquals(user.getId(), retrievedUser.getId());
    assertEquals(user.getUsername(), retrievedUser.getUsername());
    assertEquals(user.getEmail(), retrievedUser.getEmail());
    assertEquals(user.getPasswordHash(), retrievedUser.getPasswordHash());
    assertEquals(user.getType(), retrievedUser.getType());
  }

  @Test 
  void findAll_RetrievesAllUsers() {
    List<User> userList = new ArrayList<>();
    userList.add(new Student("username1", "email@provider.com", "hash", new HashSet<>()));
    userList.add(new Student("username2", "email@provider.com", "hash", new HashSet<>()));
    userList.add(new Student("username3", "email@provider.com", "hash", new HashSet<>()));
    jpa.saveAllAndFlush(userList); // Use JPA here to avoid relying on a seperate adapter method

    List<IUser> savedUsers = userRepository.findAll();
    assertEquals(3, savedUsers.size());
  }

  @Test 
  void findAll_ReturnsEmptyList_ForEmptyRepository() {
    assertTrue(userRepository.findAll().isEmpty());
  }
}
