package york.studentevents.repository;

import java.util.UUID;

/**
 * Represents an entity which can be stored in an {@link IRepository}.
 * 
 * <p>This common interface enforced that all entities have a UUID which can be used as a key.
 */
public interface IEntity {

  /** Retrieves the unique identifier of this {@code Entity}. */
  UUID getId();
  
}
