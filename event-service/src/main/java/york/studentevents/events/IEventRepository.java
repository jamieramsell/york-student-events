package york.studentevents.events;
import java.util.List;

public interface IEventRepository {
    void save(Event event);
    List<Event> findAll();
}
