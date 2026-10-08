package york.studentevents.repository;

import java.util.UUID;

/**
 * Represents an entity which can be stored in an {@link IRepository}.
 * 
 * <p>This common interface enforces that all entities have a UUID which can be used as a key.
 */
public abstract class IEntity {

  /** Retrieves the unique identifier of this {@code Entity}. */
  public abstract UUID getId();

  @Override 
  public int hashCode() {
    return getId().hashCode();
  }

  @Override 
  public boolean equals(Object other) {
    if (!(other instanceof IEntity)) {
      return false;
    }
    IEntity otherEntity = (IEntity) other;
    return getId().equals(otherEntity.getId());
  }
  
}
