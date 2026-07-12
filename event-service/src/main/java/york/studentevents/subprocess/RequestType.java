package york.studentevents.subprocess;

/**
 * Enumerates the types of requests that can be sent to the subprocess.
 *
 * <p>Each constant carries the string value used in the JSON {@code requestType} field of a
 * subprocess request.
 *
 * @see SubprocessRequestFactory
 */
public enum RequestType {

  /** Request the badges awarded to a user. */
  GET_USER_BADGES("GET_USER_BADGES"),

  /** Request the friends of a user. */
  GET_USER_FRIENDS("GET_USER_FRIENDS"),

  /** Request that a badge be awarded to a user. */
  AWARD_BADGE("AWARD_BADGE"),

  /** Request a list of IDs of the events which a user has signed up for. */
  GET_USER_EVENTS("GET_USER_EVENTS"),

  /** Request the event recommendations for a user. */
  GET_RECOMMENDED_EVENTS("GET_RECOMMENDED_EVENTS"),
  
  /** Request that a user's attendance to a particular event be recorded. */
  RECORD_ATTENDANCE("RECORD_ATTENDANCE"),

  /** Request the new friend recommendations for a user. */
  GET_RECOMMENDED_FRIENDS("GET_RECOMMENDED_FRIENDS");

  private final String request;

  /**
   * Constructor for the RequestType enum.
   *
   * @param request the string value of the request type used in the JSON {@code requestType} field
   */
  private RequestType(String request) {
    this.request = request;
  }

  /**
   * Returns the string value of this request type.
   *
   * @return the string value used in the JSON {@code requestType} field
   */
  public String getRequest() {
    return request;
  }

}
