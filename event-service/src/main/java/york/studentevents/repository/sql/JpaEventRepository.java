package york.studentevents.repository.sql;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import york.studentevents.events.Event;

@Repository
public interface JpaEventRepository extends JpaRepository<Event, UUID> {}
