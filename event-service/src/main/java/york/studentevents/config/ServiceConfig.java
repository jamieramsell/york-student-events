package york.studentevents.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import york.studentevents.cohorts.CohortService;
import york.studentevents.cohorts.ICohortRepository;
import york.studentevents.events.EventCapacityService;
import york.studentevents.events.EventService;
import york.studentevents.events.HostEventService;
import york.studentevents.events.IEventRepository;
import york.studentevents.events.StudentEventService;
import york.studentevents.subscriptions.ISubscriptionRepository;
import york.studentevents.subscriptions.SubscriptionService;
import york.studentevents.users.IUserRepository;
import york.studentevents.users.UserService;
import york.studentevents.venues.IVenueRepository;
import york.studentevents.venues.VenueService;

/**
 * Spring composition root for the service layer: constructs the domain services and wires them to
 * one another and to the repository beans supplied by {@link RepositoryConfig}.
 *
 * <p>These recipes are backend-agnostic and so carry no {@code @Profile}, with each service only
 * depending on repository interfaces, so no matter which implementation, the active profile
 * selected is injected transparently. Services that depend on other services (for example
 * {@code StudentEventService} on {@code EventService} and {@code SubscriptionService}) receive
 * those as injected beans, letting Spring resolve the whole dependency graph.
 *
 * <p>This mirrors api-core's {@code bootstrap} composition root.
 *
 * @see RepositoryConfig
 */
@Configuration 
class ServiceConfig {

  // Cohorts //

  @Bean
  CohortService cohortService(
      ICohortRepository cohortRepository,
      IUserRepository userRepository,
      IEventRepository eventRepository
  ) {
    return new CohortService(cohortRepository, userRepository, eventRepository);
  }

  // Events //

  @Bean
  EventService eventService(
      IEventRepository eventRepository
  ) {
    return new EventService(eventRepository);
  }

  @Bean 
  StudentEventService studentEventService(
      IEventRepository eventRepository,
      IUserRepository userRepository,
      SubscriptionService subscriptionService,
      EventService eventService
  ) {
    return new StudentEventService(
        eventRepository,
        userRepository,
        subscriptionService,
        eventService
    );
  }

  @Bean
  HostEventService hostEventService(
      IUserRepository userRepository,
      EventService eventService
  ) {
    return new HostEventService(userRepository, eventService);
  }

  @Bean 
  EventCapacityService eventCapacityService(
      EventService eventService,
      StudentEventService studentEventService,
      VenueService venueService
  ) {
    return new EventCapacityService(eventService, studentEventService, venueService);
  }

  // Subscriptions //

  @Bean 
  SubscriptionService subscriptionService(
      ISubscriptionRepository subscriptionRepository
  ) {
    return new SubscriptionService(subscriptionRepository);
  }

  // Users //

  @Bean 
  UserService userService(
      IUserRepository userRepository
  ) {
    return new UserService(userRepository);
  }

  // Venues //

  @Bean 
  VenueService venueService(
      IVenueRepository venueRepository
  ) {
    return new VenueService(venueRepository);
  }

}
