package york.studentevents.exceptions;

public class EventNotFoundException extends RuntimeException {
    
    public EventNotFoundException(String message) {
        super(message);
    }

    public EventNotFoundException() {
        super();
    }

}
