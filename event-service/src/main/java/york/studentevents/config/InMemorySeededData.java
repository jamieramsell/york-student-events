package york.studentevents.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import york.studentevents.cohorts.Cohort;
import york.studentevents.cohorts.ICohort;
import york.studentevents.cohorts.ICohortRepository;
import york.studentevents.events.Event;
import york.studentevents.events.EventCategory;
import york.studentevents.events.IEvent;
import york.studentevents.events.IEventRepository;
import york.studentevents.subscriptions.ISubscription;
import york.studentevents.subscriptions.ISubscriptionRepository;
import york.studentevents.subscriptions.Subscription;
import york.studentevents.users.Host;
import york.studentevents.users.IUser;
import york.studentevents.users.IUserRepository;
import york.studentevents.users.Student;
import york.studentevents.venues.IVenue;
import york.studentevents.venues.IVenueRepository;
import york.studentevents.venues.Venue;

/**
 * Populates the in-memory repositories with seed data from {@code data/seed.json} on startup.
 *
 * <p>This runner is active only under the {@code inmemory} Spring profile. It reads the shared
 * seed file (which is also consumed by {@code api-core}), deserialises the event-service entity
 * arrays (venues, users, events, cohorts, subscriptions), and saves them into the injected
 * repositories so that the service boots with a realistic data set.
 *
 * @see org.springframework.boot.CommandLineRunner
 */
@Component
@Profile("inmemory")
public class InMemorySeededData implements CommandLineRunner {

  @Autowired
  ICohortRepository cohortRepository;

  @Autowired
  IEventRepository eventRepository;

  @Autowired
  ISubscriptionRepository subscriptionRepository;

  @Autowired
  IUserRepository userRepository;

  @Autowired
  IVenueRepository venueRepository;

  /**
   * Reads {@code data/seed.json}, parses it as a JSON object, and delegates to the per-entity
   * loader methods to populate each repository.
   *
   * {@inheritDoc}
   *
   * @throws FileNotFoundException if the {@code data/seed.json} file could not be found relative
   *     to the service's working directory.
   * @throws IllegalArgumentException if the file does not contain a valid JSON object.
   */
  @Override
  public void run(String... args) throws FileNotFoundException {

    String json = "";
    File myObj = new File("../data/seed.json");

    // try-with-resources: Scanner will be closed automatically
    try (Scanner myReader = new Scanner(myObj)) {
      while (myReader.hasNextLine()) {
        String data = myReader.nextLine();
        json += data;
      }
    }

    /*
     * Try to parse the given String into a JsonObject. If this throws an error, then the String
     * provided is either not valid JSON, or is valid JSON, but is not an Object (e.g. an array or a
     * string).
     */
    JsonObject root;

    try {
      root = JsonParser.parseString(json).getAsJsonObject();
    } catch (JsonSyntaxException | IllegalStateException e) {
      throw new IllegalArgumentException("seed.json contains incorrectly formatted json.");
    }

    // Call all loaders required to load the relevant data into the java domain. Python-side data is
    // left for the python side to handle.
    cohortLoader(root.getAsJsonArray("cohorts"));
    eventLoader(root.getAsJsonArray("events"));
    subscriptionLoader(root.getAsJsonArray("subscriptions"));
    userLoader(root.getAsJsonArray("users"));
    venueLoader(root.getAsJsonArray("venues"));
  }

  // Loader Methods //

  /**
   * Deserialises each element in the given JSON array into a {@link Cohort} and saves it to the
   * cohort repository.
   *
   * @param cohorts the {@code "cohorts"} array from the seed file.
   */
  private void cohortLoader(JsonArray cohorts) {
    for (JsonElement cohortElement : cohorts) {
      JsonObject cohortJson = cohortElement.getAsJsonObject();

      ICohort cohort = new Cohort(
          UUID.fromString(cohortJson.get("id").getAsString()),
          cohortJson.get("name").getAsString(),
          cohortJson.get("department").getAsString(),
          cohortJson.get("academicYear").getAsInt(),
          cohortJson.get("yearGroup").getAsInt() // also referred to as 'stage'
      );

      // Add each member of the cohort
      for (JsonElement uuidJsonElement : cohortJson.getAsJsonArray("members")) {
        UUID memberId = UUID.fromString(uuidJsonElement.getAsString());
        cohort.addMember(memberId);
      }

      cohortRepository.save(cohort);
    }
  }

  /**
   * Deserialises each element in the given JSON array into an {@link Event} and saves it to the
   * event repository. The {@code capacity} field is set separately as it may be {@code null}.
   *
   * @param events the {@code "events"} array from the seed file.
   */
  private void eventLoader(JsonArray events) {
    for (JsonElement eventElement : events) {
      JsonObject eventJson = eventElement.getAsJsonObject();

      IEvent event = new Event(
          UUID.fromString(eventJson.get("id").getAsString()),
          eventJson.get("title").getAsString(),
          EventCategory.valueOf(eventJson.get("category").getAsString())
      );

      // Set these attributes outside of the constructor as they may be null
      if (eventJson.has("capacity")) {
        event.setCapacity(eventJson.get("capacity").getAsInt());
      }
      if (eventJson.has("description")) {
        event.setDescription(eventJson.get("description").getAsString());
      }
      if (eventJson.has("venueId")) {
        event.setVenue(UUID.fromString(eventJson.get("venueId").getAsString()));
      }
      if (eventJson.has("startDateTime") && eventJson.has("endDateTime")) {
        event.setDateTime(
            LocalDateTime.parse(eventJson.get("startDateTime").getAsString()),
            LocalDateTime.parse(eventJson.get("endDateTime").getAsString())
        );
      }

      eventRepository.save(event);
    }
  }

  /**
   * Deserialises each element in the given JSON array into a {@link Subscription} and saves it to
   * the subscription repository.
   *
   * @param subscriptions the {@code "subscriptions"} array from the seed file.
   */
  private void subscriptionLoader(JsonArray subscriptions) {
    for (JsonElement subElement : subscriptions) {
      JsonObject subJson = subElement.getAsJsonObject();

      ISubscription subscription = new Subscription(
          UUID.fromString(subJson.get("id").getAsString()),
          UUID.fromString(subJson.get("userId").getAsString()),
          UUID.fromString(subJson.get("eventId").getAsString()),
          ISubscription.SubscriptionSource.valueOf(subJson.get("source").getAsString())
      );

      subscriptionRepository.save(subscription);
    }
  }

  /**
   * Deserialises each element in the given JSON array into either a {@link Student} or a
   * {@link Host} (determined by the {@code "type"} field) and saves it to the user repository.
   *
   * @param users the {@code "users"} array from the seed file.
   */
  private void userLoader(JsonArray users) {
    for (JsonElement userElement : users) {
      JsonObject userJson = userElement.getAsJsonObject();

      UUID id = UUID.fromString(userJson.get("id").getAsString());
      String username = userJson.get("username").getAsString();
      String email = userJson.get("email").getAsString();
      String passwordHash = userJson.get("passwordHash").getAsString();
      IUser.UserType userType = IUser.UserType.valueOf(userJson.get("type").getAsString());
      Set<UUID> events = userJson.getAsJsonArray("events")
          .asList().stream()
          .map((jsonElement) -> UUID.fromString(jsonElement.getAsString()))
          .collect(Collectors.toSet());

      IUser user = switch (userType) {
        case STUDENT -> user = new Student(id, username, email, passwordHash, events);
        case HOST -> user = new Host(id, username, email, passwordHash, events);
      };

      userRepository.save(user);
    }
  }

  /**
   * Deserialises each element in the given JSON array into a {@link Venue} and saves it to the
   * venue repository. The {@code capacity} field is set separately as it may be {@code null}.
   *
   * @param venues the {@code "venues"} array from the seed file.
   */
  private void venueLoader(JsonArray venues) {
    for (JsonElement venueElement : venues) {
      JsonObject venueJson = venueElement.getAsJsonObject();

      IVenue venue = new Venue(
          UUID.fromString(venueJson.get("id").getAsString()),
          venueJson.get("name").getAsString(),
          venueJson.get("address").getAsString()
      );

      // Set the capacity outside of the constructor as it may be null
      // Set the capacity outside of the constructor as it may be null
      if (venueJson.has("capacity")) {
        venue.setCapacity(venueJson.get("capacity").getAsInt());
      }

      venueRepository.save(venue);
    }
  }

}
