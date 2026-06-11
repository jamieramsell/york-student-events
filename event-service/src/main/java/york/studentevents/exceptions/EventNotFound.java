package york.studentevents.exceptions;

public class EventNotFound extends RuntimeException {
    
    public EventNotFound(String message) {
        super(message);
    }

    public EventNotFound() {
        super();
    }

}
