package york.studentevents.users;

import york.studentevents.repository.IRepository;

/**
 * Repository for storing and retrieving {@link IUser} entities.
 *
 * <p>Extends {@link york.studentevents.repository.IRepository} with {@link IUser} as the
 * managed type, providing standard CRUD operations scoped to users.
 *
 * @see york.studentevents.repository.IRepository
 * @see IUser
 */
public interface IUserRepository extends IRepository<IUser> {}
