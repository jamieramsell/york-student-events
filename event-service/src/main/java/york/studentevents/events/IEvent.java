package york.studentevents.events;

import java.time.LocalDateTime;

public interface IEvent {

  // Getters //

  long getId();

  String getTitle();

  String getDescription();

  LocalDateTime getStartDateTime();

  LocalDateTime getEndDateTime();

  String getLocation(); // todo: implement venues

  Integer getCapacity();

  String getCategory(); // todo: implement categories

  // Setters //

  void setTitle(String title);

  void setDescription(String description);

  void setDateTime(LocalDateTime startDateTime, LocalDateTime endDateTime);

  void setLocation(String location); // todo: implement venues

  void setCapacity(Integer capacity);

  void setCategory(String category); // todo: implement categories

}
