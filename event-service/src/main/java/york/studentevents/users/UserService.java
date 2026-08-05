package york.studentevents.users;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Predicate;
import york.studentevents.exceptions.UserNotFoundException;

/**
 * Application service exposing user account related business operations.
 *
 * <p>This service forms the {@code Service} layer of the stack, sitting between the
 * {@link UserController} and the {@link IUserRepository}. Persistence is delegated to the
 * injected repository; the service holds no state of its own.
 *
 * @see IUserRepository
 * @see IUser
 */
public class UserService {

  private final IUserRepository repository;

  /**
   * Constructs a {@code UserService} backed by the given repository.
   *
   * @param repositoryInjection the repository used to store and retrieve events; must not be
   *     {@code null}
   */
  public UserService(IUserRepository repositoryInjection) {
    if (repositoryInjection == null) {
      throw new IllegalArgumentException("repositoryInjection must not be null");
    }
    this.repository = repositoryInjection;
  }

  /**
   * Retrieves a User from the injected UserRepository by their UUID.
   *
   * @param userId the user's ID
   * @return the {@code IUser} entity; never null
   * @throws IllegalArgumentException if the given ID is null
   * @throws UserNotFoundException if the given User does not exist
   */
  public IUser getUserById(UUID userId) {
    if (userId == null) {
      throw new IllegalArgumentException("userId cannot be null");
    }
    return repository.findByID(userId).orElseThrow(UserNotFoundException::new);
  }

  /**
   * Retrieves a User from the injected UserRepository by their username.
   *
   * @param username the user's username
   * @return the {@code IUser} entity; never null
   * @throws IllegalArgumentException if username is {@code null}
   * @throws UserNotFoundException if the given User does not exist
   */
  public IUser getUserByUsername(String username) {
    if (username == null) {
      throw new IllegalArgumentException("username cannot be null");
    }
    Predicate<IUser> usernameMatches = user -> user.getUsername().equals(username);

    return repository.findAll()
        .stream()
        .filter(usernameMatches)
        .findFirst()
        .orElseThrow(UserNotFoundException::new);
  }

  /**
   * Retrieves a User from the injected UserRepository by their email.
   *
   * @param email the user's email address
   * @return the {@code IUser} entity; never null
   * @throws IllegalArgumentException if email is {@code null}.
   * @throws UserNotFoundException if the given User does not exist
   */
  public IUser getUserByEmail(String email) {
    if (email == null) {
      throw new IllegalArgumentException("email cannot be null");
    }
    Predicate<IUser> emailMatches = user -> user.getEmail().equals(email);

    return repository.findAll()
        .stream()
        .filter(emailMatches)
        .findFirst()
        .orElseThrow(UserNotFoundException::new);
  }

  /**
   * Retrieves every User currently held by the backing repository.
   *
   * @return a {@link List} of all Users; never {@code null}, but may be empty if no Users have
   *     been saved
   */
  public List<IUser> getAllUsers() {
    return repository.findAll();
  }

  /**
   * Retrieves all Users of the given type (e.g. all Students)
   *
   * @param type The type of users to retrieve
   * @return a {@link List} of all Users; never {@code null}, but may be empty if no users of the
   *     given type have been saved
   *
   * @see IUser.UserType
   */
  public List<IUser> getUsersByType(IUser.UserType type) {
    return repository.findAll()
        .stream()
        .filter(user -> user.getType().equals(type))
        .toList();
  }

  /**
   * Deletes a User from the injected UserRepository.
   *
   * @param userId the user's ID
   * @throws UserNotFoundException if the given User does not exist
   */
  public void deleteUser(UUID userId) {
    try {
      repository.delete(userId);
    } catch (NoSuchElementException e) {
      throw new UserNotFoundException();
    }
  }

  /**
   * Creates and stores a record of a Student.
   *
   * @param username the Student's username
   * @param email the Student's email address
   * @param password the Student's plaintext password
   * @return a copy of the created {@code IStudent} entity.
   *
   * @throws IllegalArgumentException if any argument is null, or if an invalid password is given.
   */
  public IStudent createStudent(String username, String email, String password) {
    String passwordHash = "hashed password stub"; // TODO - implement password hashing
    IStudent student = new Student(username, email, passwordHash, new HashSet<>());

    repository.save(student);
    return student;
  }

  /**
   * Creates and stores a record of a Host.
   *
   * @param username the Host's username
   * @param email the Host's email address
   * @param password the Host's plaintext password
   * @return a copy of the created {@code IHost} entity.
   *
   * @throws IllegalArgumentException if any argument is null, or if an invalid password is given.
   */
  public IHost createHost(String username, String email, String password) {
    String passwordHash = "hashed password stub"; // TODO - implement password hashing
    IHost host = new Host(username, email, passwordHash, new HashSet<>());

    repository.save(host);
    return host;
  }

  /**
   * Retrieves a User record, and updates its username field.
   *
   * @param id The ID of the target User.
   * @param username The new username; must not be null or empty.
   * @return A copy of the updated User record.
   *
   * @throws UserNotFoundException if a User with the given ID could not be found.
   * @throws IllegalArgumentException if username is null or empty.
   */
  public IUser updateUserUsername(UUID id, String username) {
    IUser user = getUserById(id);
    user.setUsername(username);
    repository.save(user);
    return user;
  }

  /**
   * Retrieves a User record, and updates its email field.
   *
   * @param id The ID of the target User.
   * @param email The new email; must not be null or empty.
   * @return A copy of the updated User record.
   *
   * @throws UserNotFoundException if a User with the given ID could not be found.
   * @throws IllegalArgumentException if email is null or empty, or has already been registered to
   *     another account.
   */
  public IUser updateUserEmail(UUID id, String email) {
    try {
      IUser existing = getUserByEmail(email);
      if (!existing.getId().equals(id)) {
        throw new IllegalArgumentException("The given email has already been registered to an"
            + " account.");
      }
    } catch (UserNotFoundException e) {
      // No account holds this email yet, so it is free to assign to the target user.
    }
    IUser user = getUserById(id);
    user.setEmail(email);
    repository.save(user);
    return user;
  }

}