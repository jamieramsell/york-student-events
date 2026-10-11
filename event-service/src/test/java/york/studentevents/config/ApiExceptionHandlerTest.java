package york.studentevents.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import york.studentevents.exceptions.CapacityExceededException;
import york.studentevents.exceptions.CohortNotFoundException;
import york.studentevents.exceptions.ConflictException;
import york.studentevents.exceptions.EntityNotFoundException;
import york.studentevents.exceptions.EventNotFoundException;
import york.studentevents.exceptions.MissingVenueException;
import york.studentevents.exceptions.SubprocessException;
import york.studentevents.exceptions.UserNotAuthorisedException;
import york.studentevents.exceptions.UserNotFoundException;
import york.studentevents.exceptions.VenueNotFoundException;

class ApiExceptionHandlerTest {

  private static final String PATH = "/api/v1/events/123";
  private static final String SECRET = "SECRET-INTERNAL-DETAIL";

  private ApiExceptionHandler handler;
  private MockHttpServletRequest request;
  private ListAppender<ILoggingEvent> logAppender;
  private Logger handlerLogger;

  @BeforeEach
  void setUp() {
    handler = new ApiExceptionHandler();
    request = new MockHttpServletRequest("GET", PATH);

    handlerLogger = (Logger) LoggerFactory.getLogger(ApiExceptionHandler.class);
    logAppender = new ListAppender<>();
    logAppender.start();
    handlerLogger.addAppender(logAppender);
  }

  @AfterEach
  void tearDown() {
    handlerLogger.detachAppender(logAppender);
  }

  // --- Table of the simple, message-passthrough handlers (A1, A3-A9) ---

  @FunctionalInterface
  private interface Invoker {
    ResponseEntity<ApiErrorResponse> invoke(
        ApiExceptionHandler handler, RuntimeException ex, HttpServletRequest request
    );
  }

  private record Case(
      String name,
      Function<String, RuntimeException> factory,
      Invoker invoker,
      int status,
      ApiErrorCode code
  ) {
    @Override
    public String toString() {
      return name;
    }
  }

  static Stream<Case> simpleHandlerCases() {
    Invoker notFound =
        (h, ex, req) -> h.handleEntityNotFoundException((EntityNotFoundException) ex, req);
    return Stream.of(
        new Case("EventNotFound", EventNotFoundException::new, notFound, 404,
            ApiErrorCode.EVENT_NOT_FOUND),
        new Case("UserNotFound", UserNotFoundException::new, notFound, 404,
            ApiErrorCode.USER_NOT_FOUND),
        new Case("VenueNotFound", VenueNotFoundException::new, notFound, 404,
            ApiErrorCode.VENUE_NOT_FOUND),
        new Case("CohortNotFound", CohortNotFoundException::new, notFound, 404,
            ApiErrorCode.COHORT_NOT_FOUND),
        new Case("MissingVenue", MissingVenueException::new,
            (h, ex, req) -> h.handleMissingVenueException((MissingVenueException) ex, req),
            409, ApiErrorCode.MISSING_VENUE),
        new Case("CapacityExceeded", CapacityExceededException::new,
            (h, ex, req) ->
                h.handleCapacityExceededException((CapacityExceededException) ex, req),
            409, ApiErrorCode.CAPACITY_EXCEEDED),
        new Case("UserNotAuthorised", UserNotAuthorisedException::new,
            (h, ex, req) ->
                h.handleUserNotAuthorisedException((UserNotAuthorisedException) ex, req),
            403, ApiErrorCode.USER_NOT_AUTHORISED),
        new Case("Conflict", ConflictException::new,
            (h, ex, req) -> h.handleConflictException((ConflictException) ex, req),
            409, ApiErrorCode.CONFLICT),
        new Case("IllegalArgument", IllegalArgumentException::new,
            (h, ex, req) -> h.handleIllegalArgumentException((IllegalArgumentException) ex, req),
            400, ApiErrorCode.VALIDATION_FAILED)
    );
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("simpleHandlerCases")
  void simpleHandler_returnsExpectedStatusAndCode(Case testCase) {
    ResponseEntity<ApiErrorResponse> response =
        testCase.invoker().invoke(handler, testCase.factory().apply("message"), request);

    assertThat(response.getStatusCode().value()).isEqualTo(testCase.status());
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo(testCase.code());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("simpleHandlerCases")
  void simpleHandler_passesExceptionMessageThrough(Case testCase) {
    ResponseEntity<ApiErrorResponse> response =
        testCase.invoker().invoke(
            handler, testCase.factory().apply("the specific message"), request);

    assertThat(response.getBody().error()).isEqualTo("the specific message");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("simpleHandlerCases")
  void simpleHandler_withNullMessage_stillProvidesAnErrorMessage(Case testCase) {
    ResponseEntity<ApiErrorResponse> response =
        testCase.invoker().invoke(handler, testCase.factory().apply(null), request);

    assertThat(response.getBody().error()).isNotBlank();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("simpleHandlerCases")
  void simpleHandler_setsPathAndTimestamp(Case testCase) {
    Instant before = Instant.now();

    ResponseEntity<ApiErrorResponse> response =
        testCase.invoker().invoke(handler, testCase.factory().apply("message"), request);

    Instant after = Instant.now();
    assertThat(response.getBody().path()).isEqualTo(PATH);
    assertThat(response.getBody().timestamp()).isBetween(before, after);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("simpleHandlerCases")
  void simpleHandler_hasNoFieldDetail(Case testCase) {
    ResponseEntity<ApiErrorResponse> response =
        testCase.invoker().invoke(handler, testCase.factory().apply("message"), request);

    assertThat(response.getBody().fields()).isNull();
  }

  @Test
  void entityNotFound_withUnknownSubclass_failsFast() {
    EntityNotFoundException unknown = new EntityNotFoundException("unmapped") { };

    assertThrows(IllegalStateException.class,
        () -> handler.handleEntityNotFoundException(unknown, request));
  }

  // --- SubprocessException (A10, A11) ---

  @Test
  void subprocessException_returnsBadGatewayWithGenericMessage() {
    SubprocessException ex = new SubprocessException(
        "bridge failed: " + SECRET, new IOException("cause: " + SECRET));

    ResponseEntity<ApiErrorResponse> response = handler.handleSubprocessException(ex, request);

    assertThat(response.getStatusCode().value()).isEqualTo(502);
    assertThat(response.getBody().code()).isEqualTo(ApiErrorCode.SUBPROCESS_FAILURE);
    assertThat(response.getBody().error()).isEqualTo("A downstream service failed to respond");
    assertThat(response.getBody().path()).isEqualTo(PATH);
  }

  @Test
  void subprocessException_neverLeaksInternalDetailInTheResponse() {
    SubprocessException ex = new SubprocessException(
        "bridge failed: " + SECRET, new IOException("cause: " + SECRET));

    ResponseEntity<ApiErrorResponse> response = handler.handleSubprocessException(ex, request);

    assertThat(response.getBody().toString()).doesNotContain(SECRET);
  }

  @Test
  void subprocessException_isLoggedAtErrorWithTheExceptionAttached() {
    SubprocessException ex = new SubprocessException("bridge failed", new IOException("io"));

    handler.handleSubprocessException(ex, request);

    assertThat(logAppender.list).hasSize(1);
    ILoggingEvent logged = logAppender.list.get(0);
    assertThat(logged.getLevel()).isEqualTo(Level.ERROR);
    assertThat(logged.getThrowableProxy()).isNotNull();
    assertThat(logged.getThrowableProxy().getMessage()).isEqualTo("bridge failed");
    assertThat(logged.getFormattedMessage()).contains(PATH);
  }

  // --- Catch-all (A12, A13) ---

  @Test
  void unexpectedException_returnsInternalServerErrorWithGenericMessage() {
    ResponseEntity<ApiErrorResponse> response =
        handler.handleUnexpectedException(new RuntimeException(SECRET), request);

    assertThat(response.getStatusCode().value()).isEqualTo(500);
    assertThat(response.getBody().code()).isEqualTo(ApiErrorCode.INTERNAL_ERROR);
    assertThat(response.getBody().error()).isEqualTo("An internal error occurred.");
    assertThat(response.getBody().path()).isEqualTo(PATH);
  }

  @Test
  void unexpectedException_neverLeaksInternalDetailInTheResponse() {
    ResponseEntity<ApiErrorResponse> response =
        handler.handleUnexpectedException(new RuntimeException(SECRET), request);

    assertThat(response.getBody().toString()).doesNotContain(SECRET);
  }

  @Test
  void unexpectedException_isLoggedAtErrorWithTheExceptionAttached() {
    handler.handleUnexpectedException(new RuntimeException(SECRET), request);

    assertThat(logAppender.list).hasSize(1);
    ILoggingEvent logged = logAppender.list.get(0);
    assertThat(logged.getLevel()).isEqualTo(Level.ERROR);
    assertThat(logged.getThrowableProxy()).isNotNull();
    assertThat(logged.getThrowableProxy().getMessage()).isEqualTo(SECRET);
    assertThat(logged.getFormattedMessage()).contains(PATH);
  }

  // --- handleExceptionInternal: Spring's own exceptions (A14-A18) ---

  @Test
  void handleExceptionInternal_withErrorResponseException_usesItsStatusDetailAndHeaders() {
    HttpRequestMethodNotSupportedException ex =
        new HttpRequestMethodNotSupportedException("DELETE", List.of("GET", "POST"));
    HttpHeaders headers = new HttpHeaders();
    headers.setAllow(Set.of(HttpMethod.GET, HttpMethod.POST));

    ResponseEntity<Object> response = handler.handleExceptionInternal(
        ex, null, headers, HttpStatus.METHOD_NOT_ALLOWED, new ServletWebRequest(request));

    assertThat(response.getStatusCode().value()).isEqualTo(405);
    assertThat(response.getHeaders().getAllow()).containsExactlyInAnyOrder(
        HttpMethod.GET, HttpMethod.POST);
    ApiErrorResponse body = (ApiErrorResponse) response.getBody();
    assertThat(body.code()).isEqualTo(ApiErrorCode.METHOD_NOT_ALLOWED);
    assertThat(body.error()).contains("DELETE");
    assertThat(body.path()).isEqualTo(PATH);
    assertThat(body.fields()).isNull();
  }

  @ParameterizedTest(name = "status {0} -> {1}")
  @CsvSource({
      "400, BAD_REQUEST",
      "404, ROUTE_NOT_FOUND",
      "405, METHOD_NOT_ALLOWED",
      "406, BAD_REQUEST",
      "415, UNSUPPORTED_MEDIA_TYPE",
      "500, INTERNAL_ERROR",
      "503, INTERNAL_ERROR"
  })
  void handleExceptionInternal_selectsErrorCodeFromStatus(int status, ApiErrorCode expected) {
    ResponseEntity<Object> response = handler.handleExceptionInternal(
        new Exception("irrelevant"), null, new HttpHeaders(), HttpStatusCode.valueOf(status),
        new ServletWebRequest(request));

    assertThat(response.getStatusCode().value()).isEqualTo(status);
    assertThat(((ApiErrorResponse) response.getBody()).code()).isEqualTo(expected);
  }

  @Test
  void handleExceptionInternal_withoutErrorResponseDetail_usesFallbackMessage() {
    ResponseEntity<Object> response = handler.handleExceptionInternal(
        new Exception(SECRET), null, new HttpHeaders(), HttpStatus.BAD_REQUEST,
        new ServletWebRequest(request));

    ApiErrorResponse body = (ApiErrorResponse) response.getBody();
    assertThat(body.error()).isEqualTo("The request could not be processed.");
    assertThat(body.toString()).doesNotContain(SECRET);
  }

  @Test
  void handleExceptionInternal_withServerErrorStatus_logsAtError() {
    Exception ex = new Exception("server side");

    handler.handleExceptionInternal(
        ex, null, new HttpHeaders(), HttpStatus.SERVICE_UNAVAILABLE,
        new ServletWebRequest(request));

    assertThat(logAppender.list).hasSize(1);
    assertThat(logAppender.list.get(0).getLevel()).isEqualTo(Level.ERROR);
    assertThat(logAppender.list.get(0).getThrowableProxy().getMessage()).isEqualTo("server side");
  }

  @Test
  void handleExceptionInternal_withClientErrorStatus_doesNotLog() {
    handler.handleExceptionInternal(
        new Exception("client side"), null, new HttpHeaders(), HttpStatus.BAD_REQUEST,
        new ServletWebRequest(request));

    assertThat(logAppender.list).isEmpty();
  }

  @Test
  void handleExceptionInternal_withNonServletRequest_hasNullPath() {
    WebRequest nonServletRequest = mock(WebRequest.class);

    ResponseEntity<Object> response = handler.handleExceptionInternal(
        new Exception("irrelevant"), null, new HttpHeaders(), HttpStatus.BAD_REQUEST,
        nonServletRequest);

    assertThat(((ApiErrorResponse) response.getBody()).path()).isNull();
  }

  // --- handleMethodArgumentNotValid (A19, A20) ---

  @SuppressWarnings("unused")
  private static void validatedTarget(String argument) { }

  private MethodArgumentNotValidException validationException(BeanPropertyBindingResult result)
      throws NoSuchMethodException {
    MethodParameter parameter = new MethodParameter(
        ApiExceptionHandlerTest.class.getDeclaredMethod("validatedTarget", String.class), 0);
    return new MethodArgumentNotValidException(parameter, result);
  }

  @Test
  void handleMethodArgumentNotValid_listsEveryFailedField() throws Exception {
    BeanPropertyBindingResult result = new BeanPropertyBindingResult(new Object(), "target");
    result.addError(new FieldError("target", "title", "must not be blank"));
    result.addError(new FieldError("target", "capacity", "must be positive"));

    ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(
        validationException(result), new HttpHeaders(), HttpStatus.BAD_REQUEST,
        new ServletWebRequest(request));

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    ApiErrorResponse body = (ApiErrorResponse) response.getBody();
    assertThat(body.code()).isEqualTo(ApiErrorCode.VALIDATION_FAILED);
    assertThat(body.error()).isEqualTo("Request validation failed");
    assertThat(body.path()).isEqualTo(PATH);
    assertThat(body.fields()).containsExactlyInAnyOrder(
        new ApiErrorResponse.FieldError("title", "must not be blank"),
        new ApiErrorResponse.FieldError("capacity", "must be positive"));
  }

  @Test
  void handleMethodArgumentNotValid_withOnlyGlobalErrors_hasNoFieldEntries() throws Exception {
    BeanPropertyBindingResult result = new BeanPropertyBindingResult(new Object(), "target");
    result.addError(new ObjectError("target", "class-level problem"));

    ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(
        validationException(result), new HttpHeaders(), HttpStatus.BAD_REQUEST,
        new ServletWebRequest(request));

    ApiErrorResponse body = (ApiErrorResponse) response.getBody();
    assertThat(body.code()).isEqualTo(ApiErrorCode.VALIDATION_FAILED);
    assertThat(body.fields()).isEmpty();
  }
}
