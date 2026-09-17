package york.studentevents.repository.sql;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import york.studentevents.users.User;

/**
 * JPA backed repository for storing and retrieving {@link User} entities.
 *
 * <p>Extends {@link JpaRepository} with {@link User} as the entity type, and {@link UUID} as the
 *     key type, providing standard CRUD operations scoped to Users.
 *
 * <p>Should be used in conjunction with the {@link UserRepositoryAdapter} to map methods onto the
 *     {@link york.studentevents.users.IUserRepository} interface.
 *
 * @see york.studentevents.repository.IRepository
 * @see york.studentevents.users.IUserRepository
 * @see york.studentevents.users.User
 */
@Repository
public interface JpaUserRepository extends JpaRepository<User, UUID> {}
