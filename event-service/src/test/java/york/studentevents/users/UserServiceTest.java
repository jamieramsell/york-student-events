package york.studentevents.users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import york.studentevents.exceptions.UserNotFoundException;
import york.studentevents.repository.inmemory.InMemoryUserRepository;

/**
 * Tests {@link UserService} against a real {@link InMemoryUserRepository}.
 *
 * <p>The service delegates persistence to the repository and delegates field validation to the
 * {@link User} entity's setters, so these tests exercise both the service's own behaviour (lookup,
 * error translation, filtering, persistence) and its correct wiring into the entity's validation
 * rules.
 */
class UserServiceTest {

  private InMemoryUserRepository repository;
  private UserService service;

  @BeforeEach
  void setUp() {
    repository = new InMemoryUserRepository();
    service = new UserService(repository);
  }

  // --- constructor ---

  @Test
  void constructor_withNullRepository_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> new UserService(null));
  }

  // --- getUserById ---

  @Test
  void getUserById_whenUserExists_returnsUser() {
    IUser user = savedStudent("alice", "alice@york.ac.uk");

    assertEquals(user, service.getUserById(user.getId()));
  }

  @Test
  void getUserById_withNullId_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> service.getUserById(null));
  }

  @Test
  void getUserById_whenUserDoesNotExist_throwsUserNotFoundException() {
    assertThrows(UserNotFoundException.class, () -> service.getUserById(UUID.randomUUID()));
  }

  // --- getUserByUsername ---

  @Test
  void getUserByUsername_whenUserExists_returnsMatchingUser() {
    IUser alice = savedStudent("alice", "alice@york.ac.uk");
    savedStudent("bob", "bob@york.ac.uk");

    assertEquals(alice, service.getUserByUsername("alice"));
  }

  @Test
  void getUserByUsername_withNullUsername_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> service.getUserByUsername(null));
  }

  @Test
  void getUserByUsername_whenNoUserMatches_throwsUserNotFoundException() {
    savedStudent("alice", "alice@york.ac.uk");

    assertThrows(UserNotFoundException.class, () -> service.getUserByUsername("nobody"));
  }

  // --- getUserByEmail ---

  @Test
  void getUserByEmail_whenUserExists_returnsMatchingUser() {
    IUser alice = savedStudent("alice", "alice@york.ac.uk");
    savedStudent("bob", "bob@york.ac.uk");

    assertEquals(alice, service.getUserByEmail("alice@york.ac.uk"));
  }

  @Test
  void getUserByEmail_withNullEmail_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> service.getUserByEmail(null));
  }

  @Test
  void getUserByEmail_whenNoUserMatches_throwsUserNotFoundException() {
    savedStudent("alice", "alice@york.ac.uk");

    assertThrows(UserNotFoundException.class, () -> service.getUserByEmail("ghost@york.ac.uk"));
  }

  // --- getAllUsers ---

  @Test
  void getAllUsers_whenEmpty_returnsEmptyList() {
    assertTrue(service.getAllUsers().isEmpty());
  }

  @Test
  void getAllUsers_returnsAllSavedUsers() {
    IUser alice = savedStudent("alice", "alice@york.ac.uk");
    IUser host = service.createHost("su", "su@york.ac.uk", "pw");

    List<IUser> users = service.getAllUsers();

    assertEquals(2, users.size());
    assertTrue(users.contains(alice));
    assertTrue(users.contains(host));
  }

  // --- getUsersByType ---

  @Test
  void getUsersByType_returnsOnlyUsersOfThatType() {
    IUser student = savedStudent("alice", "alice@york.ac.uk");
    IUser host = service.createHost("su", "su@york.ac.uk", "pw");

    List<IUser> students = service.getUsersByType(IUser.UserType.STUDENT);
    List<IUser> hosts = service.getUsersByType(IUser.UserType.HOST);

    assertEquals(List.of(student), students);
    assertEquals(List.of(host), hosts);
  }

  @Test
  void getUsersByType_whenNoneOfThatType_returnsEmptyList() {
    savedStudent("alice", "alice@york.ac.uk");

    assertTrue(service.getUsersByType(IUser.UserType.HOST).isEmpty());
  }

  // --- createStudent ---

  @Test
  void createStudent_createsAndPersistsStudent() {
    IStudent student = service.createStudent("alice", "alice@york.ac.uk", "pw");

    assertEquals("alice", student.getUsername());
    assertEquals("alice@york.ac.uk", student.getEmail());
    assertEquals(IUser.UserType.STUDENT, student.getType());
    assertTrue(student.getRegisteredEvents().isEmpty());
    assertEquals(student, service.getUserById(student.getId()));
  }

  @Test
  void createStudent_storesPasswordHash() {
    IStudent student = service.createStudent("alice", "alice@york.ac.uk", "pw");

    // The hash need not equal the plaintext, but a non-null hash must be persisted so the
    // NOT NULL column and any future authentication check are satisfiable.
    assertNotNull(student.getPasswordHash());
  }

  @Test
  void createStudent_withInvalidEmail_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> service.createStudent("alice", "not-an-email", "pw"));
  }

  @Test
  void createStudent_withBlankUsername_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> service.createStudent("  ", "alice@york.ac.uk", "pw"));
  }

  // --- createHost ---

  @Test
  void createHost_createsAndPersistsHost() {
    IHost host = service.createHost("su", "su@york.ac.uk", "pw");

    assertEquals("su", host.getUsername());
    assertEquals("su@york.ac.uk", host.getEmail());
    assertEquals(IUser.UserType.HOST, host.getType());
    assertTrue(host.getHostedEvents().isEmpty());
    assertEquals(host, service.getUserById(host.getId()));
  }

  @Test
  void createHost_storesPasswordHash() {
    IHost host = service.createHost("su", "su@york.ac.uk", "pw");

    assertNotNull(host.getPasswordHash());
  }

  // --- updateUserUsername ---

  @Test
  void updateUserUsername_updatesAndPersistsUsername() {
    IUser user = savedStudent("alice", "alice@york.ac.uk");

    service.updateUserUsername(user.getId(), "alice2");

    assertEquals("alice2", service.getUserById(user.getId()).getUsername());
  }

  @Test
  void updateUserUsername_withBlankUsername_throwsIllegalArgumentException() {
    IUser user = savedStudent("alice", "alice@york.ac.uk");

    assertThrows(IllegalArgumentException.class,
        () -> service.updateUserUsername(user.getId(), "  "));
  }

  @Test
  void updateUserUsername_whenUserDoesNotExist_throwsUserNotFoundException() {
    assertThrows(UserNotFoundException.class,
        () -> service.updateUserUsername(UUID.randomUUID(), "whoever"));
  }

  // --- updateUserEmail ---

  @Test
  void updateUserEmail_updatesAndPersistsEmail() {
    IUser user = savedStudent("alice", "alice@york.ac.uk");

    service.updateUserEmail(user.getId(), "alice.new@york.ac.uk");

    assertEquals("alice.new@york.ac.uk", service.getUserById(user.getId()).getEmail());
  }

  @Test
  void updateUserEmail_whenEmailBelongsToAnotherUser_throwsIllegalArgumentException() {
    savedStudent("bob", "bob@york.ac.uk");
    IUser alice = savedStudent("alice", "alice@york.ac.uk");

    assertThrows(IllegalArgumentException.class,
        () -> service.updateUserEmail(alice.getId(), "bob@york.ac.uk"));
  }

  @Test
  void updateUserEmail_toUsersOwnCurrentEmail_isAllowed() {
    IUser alice = savedStudent("alice", "alice@york.ac.uk");

    // Re-setting a user's email to the value it already holds must not be treated as a collision.
    service.updateUserEmail(alice.getId(), "alice@york.ac.uk");

    assertEquals("alice@york.ac.uk", service.getUserById(alice.getId()).getEmail());
  }

  @Test
  void updateUserEmail_withInvalidEmail_throwsIllegalArgumentException() {
    IUser user = savedStudent("alice", "alice@york.ac.uk");

    assertThrows(IllegalArgumentException.class,
        () -> service.updateUserEmail(user.getId(), "not-an-email"));
  }

  @Test
  void updateUserEmail_whenUserDoesNotExist_throwsUserNotFoundException() {
    assertThrows(UserNotFoundException.class,
        () -> service.updateUserEmail(UUID.randomUUID(), "someone@york.ac.uk"));
  }

  // --- deleteUser ---

  @Test
  void deleteUser_removesUser() {
    IUser user = savedStudent("alice", "alice@york.ac.uk");

    service.deleteUser(user.getId());

    assertFalse(service.getAllUsers().contains(user));
    assertThrows(UserNotFoundException.class, () -> service.getUserById(user.getId()));
  }

  @Test
  void deleteUser_whenUserDoesNotExist_throwsUserNotFoundException() {
    assertThrows(UserNotFoundException.class, () -> service.deleteUser(UUID.randomUUID()));
  }

  // --- helpers ---

  private IUser savedStudent(String username, String email) {
    // createStudent already persists via the repository; returned for convenience.
    IStudent student = service.createStudent(username, email, "pw");
    return student;
  }
}
