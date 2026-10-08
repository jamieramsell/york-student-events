package york.studentevents.subscriptions;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.springframework.lang.NonNull;
import york.studentevents.events.IEvent;
import york.studentevents.users.IUser;

/** Concrete implementation of {@link ISubscription}, representing a User subscribing to an Event.
 */
@Entity 
@Table(name = "student_event_subscriptions")
public class Subscription implements ISubscription {
  
  @Id
  private UUID id;

  @Column(nullable = false)
  private UUID userId;

  @Column(nullable = false)
  private UUID eventId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SubscriptionSource source;

  /**
   * Creates a {@code Subscription} between a User and an Event.
   *
   * @param userId the ID of the target user; must not be {@code null}.
   * @param eventId the ID of the target event; must not be {@code null}.
   * @param source the source of the subscription; must not be {@code null}.
   * @throws IllegalArgumentException if any of the parameters are {@code null}
   *
   * @see IUser
   * @see IEvent
   * @see SubscriptionSource
   */
  public Subscription(@NonNull UUID userId, @NonNull UUID eventId, SubscriptionSource source) {
    this(UUID.randomUUID(), userId, eventId, source);
  }

  /**
   * Creates a {@code Subscription} between a User and an Event.
   *
   * @param id the subscription ID; must not be {@code null}.
   * @param userId the ID of the target user; must not be {@code null}.
   * @param eventId the ID of the target event; must not be {@code null}.
   * @param source the source of the subscription; must not be {@code null}.
   * @throws IllegalArgumentException if any of the parameters are {@code null}
   *
   * @see IUser
   * @see IEvent
   * @see SubscriptionSource
   */
  public Subscription(@NonNull UUID id,
                      @NonNull UUID userId,
                      @NonNull UUID eventId,
                      SubscriptionSource source) {
    this.id = id;
    this.userId = userId;
    this.eventId = eventId;
    this.source = source;
  }

  /** No-args constructor for JPA use only. */
  protected Subscription() {}

  // Getters //

  @Override
  @NonNull
  public UUID getId() {
    return id;
  }

  @Override
  @NonNull
  public UUID getUserId() {
    return userId;
  }

  @Override
  @NonNull
  public UUID getEventId() {
    return eventId;
  }

  @Override
  public SubscriptionSource getSource() {
    return source;
  }
  
}
