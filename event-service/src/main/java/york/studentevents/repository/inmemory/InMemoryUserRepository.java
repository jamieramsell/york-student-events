package york.studentevents.repository.inmemory;

import york.studentevents.users.IUser;

/**
 * Hash-map backed repository for storing and retrieving {@link IUser} entities.
 *
 * <p>Extends {@link york.studentevents.repository.inmemory.AbstractInMemoryRepository} with {@link IUser}
 * as the managed type, providing standard CRUD operations scoped to users.
 *
 * <p>Used for integration testing before implementing database-backed repositories.
 * 
 * @see york.studentevents.repository.IRepository
 * @see AbstractInMemoryRepository
 * @see IUser
 */
public class InMemoryUserRepository extends AbstractInMemoryRepository<IUser> {}