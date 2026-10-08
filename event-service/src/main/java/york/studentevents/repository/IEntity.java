package york.studentevents.repository;

import java.util.UUID;
import org.springframework.lang.NonNull;

/**
 * Represents an entity which can be stored in an {@link IRepository}.
 * 
 * <p>This common interface enforces that all entities have a UUID which can be used as a key.
 */
public interface IEntity {

  /** Retrieves the unique identifier of this {@code Entity}. */
  @NonNull 
  UUID getId();
  
}
