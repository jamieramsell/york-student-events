package york.studentevents.exceptions;

public class CohortNotFoundException extends RuntimeException {
    
    public CohortNotFoundException(String message) {
        super(message);
    }

    public CohortNotFoundException() {
        super();
    }
    
}
