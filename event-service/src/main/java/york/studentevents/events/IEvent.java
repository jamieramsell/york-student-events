package york.studentevents.events;

import java.time.LocalDateTime;

public interface IEvent {

  public long getId();

  public String getTitle();

  public String getDescription();

  public LocalDateTime getDate();

  public String getLocation(); // todo: implement venues

  public int getCapacity();

  public String getCategory(); // todo: implement categories

}
