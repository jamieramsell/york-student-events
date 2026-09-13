package york.studentevents.repository.sql;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import york.studentevents.users.IUser;
import york.studentevents.users.IUserRepository;
import york.studentevents.users.User;

/**
 * Database-backed implementation of {@link IUserRepository}, adapting Spring Data JPA to the
 * domain repository contract.
 *
 * <p>Persistence is delegated to a {@link JpaUserRepository}, whose implementation is generated
 *     by Spring Data at runtime. This adapter translates between the two layers: the concrete
 *     {@link User} entity is widened to the {@link IUser} interface on the way out, a missing row
 *     is mapped to {@link Optional#empty()}, and a delete against a missing row is reported as the
 *     {@link java.util.NoSuchElementException} that the
 *     {@link york.studentevents.repository.IRepository} contract requires, rather than the
 *     framework exception Spring Data would otherwise raise.
 *
 * <p>This is the intended production implementation of {@link IUserRepository}, superseding the
 *     hash-map backed {@link york.studentevents.repository.inmemory.InMemoryUserRepository}.
 *
 * @see IUserRepository
 * @see JpaUserRepository
 * @see IUser
 */
@Repository 
public class UserRepositoryAdapter implements IUserRepository {

  private final JpaUserRepository jpa;

  /**
   * Constructs an adapter that delegates persistence to the given Spring Data repository.
   *
   * @param jpa the Spring Data repository to delegate to; must not be {@code null}
   */
  public UserRepositoryAdapter(JpaUserRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public void save(IUser entity) {
    User concreteEntity = (User) entity;
    if (concreteEntity == null) {
      throw new IllegalArgumentException("entity cannot be null");
    }
    jpa.saveAndFlush(concreteEntity);
  }

  @Override
  public void delete(UUID id) {
    if (id == null) {
      throw new IllegalArgumentException("id cannot be null");
    }
    if (!jpa.existsById(id)) {
      throw new NoSuchElementException("No entity with the given ID exists within the database.");
    }
    jpa.deleteById(id);
  }

  @Override
  public Optional<IUser> findByID(UUID id) {
    if (id == null) {
      throw new IllegalArgumentException("id cannot be null");
    }
    Optional<User> concreteUser = jpa.findById(id);

    // Cast the concrete user to its interface
    Optional<IUser> interfaceUser;
    try {
      interfaceUser = Optional.of((IUser) concreteUser.get());
    } catch (NoSuchElementException e) {
      interfaceUser = Optional.empty();
    }
    return interfaceUser;
  }

  @Override
  public List<IUser> findAll() {
    return jpa.findAll()
        .stream()
        .map((e) -> (IUser) e)
        .toList();
  }
}
