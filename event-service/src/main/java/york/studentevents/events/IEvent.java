package york.studentevents.events;

import java.time.LocalDateTime;

public interface IEvent {

  long getId();

  String getTitle();

  String getDescription();

  LocalDateTime getStartDateTime();

  LocalDateTime getEndDateTime();

  String getLocation(); // todo: implement venues

  int getCapacity();

  String getCategory(); // todo: implement categories

}
