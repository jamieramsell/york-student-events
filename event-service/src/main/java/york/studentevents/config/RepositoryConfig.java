package york.studentevents.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import york.studentevents.cohorts.ICohortRepository;
import york.studentevents.events.IEventRepository;
import york.studentevents.repository.inmemory.InMemoryCohortRepository;
import york.studentevents.repository.inmemory.InMemoryEventRepository;
import york.studentevents.repository.inmemory.InMemorySubscriptionRepository;
import york.studentevents.repository.inmemory.InMemoryUserRepository;
import york.studentevents.repository.inmemory.InMemoryVenueRepository;
import york.studentevents.repository.sql.CohortRepositoryAdapter;
import york.studentevents.repository.sql.EventRepositoryAdapter;
import york.studentevents.repository.sql.JpaCohortRepository;
import york.studentevents.repository.sql.JpaEventRepository;
import york.studentevents.repository.sql.JpaUserRepository;
import york.studentevents.repository.sql.UserRepositoryAdapter;
import york.studentevents.subscriptions.ISubscriptionRepository;
import york.studentevents.users.IUserRepository;
import york.studentevents.venues.IVenueRepository;

/**
 * Spring composition root for the repository layer: the single place that selects which
 * {@link york.studentevents.repository.IRepository} implementation backs each domain slice.
 *
 * <p>Slices with both a persistent and an in-memory implementation are profile-scoped, as the
 * database-backed adapters are supplied by default, and the in-memory equivalents are under the
 * {@code inmemory} profile (activated for database-free runs, such as the bridge responder's test
 * mode). A slice with only one implementation is declared unconditionally.
 *
 * <p>This is the event-service counterpart of api-core's {@code bootstrap} / {@code bootstrap_sql}
 * repository selection.
 *
 * @see ServiceConfig
 */
@Configuration 
class RepositoryConfig {
  
  // Cohorts //

  @Bean 
  @Profile("!inmemory")
  ICohortRepository cohortRepository(JpaCohortRepository jpa) {
    return new CohortRepositoryAdapter(jpa);
  }

  @Bean 
  @Profile("inmemory")
  ICohortRepository inMemoryCohortRepository() {
    return new InMemoryCohortRepository();
  }

  // Events //

  @Bean 
  @Profile("!inmemory")
  IEventRepository eventRepository(JpaEventRepository jpa) {
    return new EventRepositoryAdapter(jpa);
  }

  @Bean 
  @Profile("inmemory")
  IEventRepository inMemoryEventRepository() {
    return new InMemoryEventRepository();
  }

  // Subscriptions //

  @Bean 
  ISubscriptionRepository inMemorySubscriptionRepository() {
    return new InMemorySubscriptionRepository();
  }

  // Users //

  @Bean 
  @Profile("!inmemory")
  IUserRepository userRepository(JpaUserRepository jpa) {
    return new UserRepositoryAdapter(jpa);
  }

  @Bean 
  @Profile("inmemory")
  IUserRepository inMemoryUserRepository() {
    return new InMemoryUserRepository();
  }

  // Venues //

  @Bean
  IVenueRepository inMemoryVenueRepository() {
    return new InMemoryVenueRepository();
  }

}
