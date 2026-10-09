package york.studentevents.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import york.studentevents.exceptions.CapacityExceededException;
import york.studentevents.exceptions.CohortNotFoundException;
import york.studentevents.exceptions.ConflictException;
import york.studentevents.exceptions.EventNotFoundException;
import york.studentevents.exceptions.MissingVenueException;
import york.studentevents.exceptions.SubprocessException;
import york.studentevents.exceptions.UserNotAuthorisedException;
import york.studentevents.exceptions.UserNotFoundException;
import york.studentevents.exceptions.VenueNotFoundException;

/**
 * Proves that {@link ApiExceptionHandler} is registered with Spring MVC and that real requests are
 * translated into the standard {@link ApiErrorResponse} shape.
 *
 * <p>Uses a test-only stub controller so that every kind of exception can be triggered, including
 * ones no real endpoint throws yet.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("inmemory")
class ApiErrorWiringTest {

  private static final String SECRET = "SECRET-INTERNAL-DETAIL";

  /** Body used by the stub's validated endpoint. */
  record StubBody(@NotBlank String name, @Min(1) Integer age) { }

  /**
   * Registered through {@link StubConfiguration}. As a nested class of a test class it is excluded
   * from component scanning, so it never appears in other tests' application contexts.
   */
  @RestController
  @RequestMapping("/test-errors")
  public static class StubController {

    @GetMapping("/{kind}")
    public String throwByKind(@PathVariable String kind) {
      throw switch (kind) {
        case "event-not-found" -> new EventNotFoundException("boom: " + kind);
        case "user-not-found" -> new UserNotFoundException("boom: " + kind);
        case "venue-not-found" -> new VenueNotFoundException("boom: " + kind);
        case "cohort-not-found" -> new CohortNotFoundException("boom: " + kind);
        case "missing-venue" -> new MissingVenueException("boom: " + kind);
        case "capacity" -> new CapacityExceededException("boom: " + kind);
        case "not-authorised" -> new UserNotAuthorisedException("boom: " + kind);
        case "conflict" -> new ConflictException("boom: " + kind);
        case "illegal-argument" -> new IllegalArgumentException("boom: " + kind);
        case "subprocess" -> new SubprocessException("boom: " + kind + SECRET,
            new IOException(SECRET));
        default -> new RuntimeException(SECRET);
      };
    }

    @PostMapping(value = "/validated", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String validated(@Valid @RequestBody StubBody body) {
      return body.name();
    }

    @GetMapping("/param")
    public String param(@RequestParam int count) {
      return "ok " + count;
    }
  }

  /** Registers the stub controller for this test class only. */
  @TestConfiguration
  static class StubConfiguration {
    @Bean
    StubController stubController() {
      return new StubController();
    }
  }

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  private JsonNode bodyOf(MvcResult result) throws Exception {
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private Set<String> keysOf(JsonNode node) {
    Set<String> keys = new HashSet<>();
    node.fieldNames().forEachRemaining(keys::add);
    return keys;
  }

  // --- B1: every exception kind, through real MVC routing ---

  @ParameterizedTest(name = "{0} -> {1} {2}")
  @CsvSource({
      "event-not-found, 404, EVENT_NOT_FOUND",
      "user-not-found, 404, USER_NOT_FOUND",
      "venue-not-found, 404, VENUE_NOT_FOUND",
      "cohort-not-found, 404, COHORT_NOT_FOUND",
      "missing-venue, 409, MISSING_VENUE",
      "capacity, 409, CAPACITY_EXCEEDED",
      "not-authorised, 403, USER_NOT_AUTHORISED",
      "conflict, 409, CONFLICT",
      "illegal-argument, 400, VALIDATION_FAILED",
      "subprocess, 502, SUBPROCESS_FAILURE",
      "unexpected, 500, INTERNAL_ERROR"
  })
  void domainException_isTranslatedToExpectedStatusAndCode(
      String kind, int expectedStatus, String expectedCode
  ) throws Exception {
    MvcResult result = mockMvc.perform(get("/test-errors/" + kind))
        .andExpect(status().is(expectedStatus))
        .andReturn();

    assertThat(bodyOf(result).get("code").asText()).isEqualTo(expectedCode);
  }

  // --- B2-B4: shape of a standard error body ---

  @Test
  void errorBody_hasExactlyTheFourStandardKeys() throws Exception {
    MvcResult result = mockMvc.perform(get("/test-errors/event-not-found")).andReturn();

    assertThat(keysOf(bodyOf(result)))
        .containsExactlyInAnyOrder("error", "code", "timestamp", "path");
  }

  @Test
  void errorBody_timestampIsAnIsoUtcInstant() throws Exception {
    MvcResult result = mockMvc.perform(get("/test-errors/event-not-found")).andReturn();

    String timestamp = bodyOf(result).get("timestamp").asText();
    assertThat(timestamp).endsWith("Z");
    assertThat(Instant.parse(timestamp)).isNotNull();
  }

  @Test
  void errorBody_pathIsTheRequestUri() throws Exception {
    MvcResult result = mockMvc.perform(get("/test-errors/event-not-found")).andReturn();

    assertThat(bodyOf(result).get("path").asText()).isEqualTo("/test-errors/event-not-found");
  }

  // --- B5: nothing internal leaks ---

  @Test
  void unexpectedException_doesNotLeakMessageOrStackTrace() throws Exception {
    MvcResult result = mockMvc.perform(get("/test-errors/unexpected")).andReturn();

    String body = result.getResponse().getContentAsString();
    assertThat(body).doesNotContain(SECRET).doesNotContain("at york.").doesNotContain("Exception");
  }

  @Test
  void subprocessException_doesNotLeakMessageOrCause() throws Exception {
    MvcResult result = mockMvc.perform(get("/test-errors/subprocess")).andReturn();

    String body = result.getResponse().getContentAsString();
    assertThat(body).doesNotContain(SECRET).doesNotContain("IOException");
  }

  // --- B6-B12: Spring's own client errors must not become 500s ---

  @Test
  void unknownRoute_returnsNotFoundWithRouteNotFoundCode() throws Exception {
    MvcResult result = mockMvc.perform(get("/api/v1/definitely-not-a-route"))
        .andExpect(status().isNotFound())
        .andReturn();

    JsonNode body = bodyOf(result);
    assertThat(body.get("code").asText()).isEqualTo("ROUTE_NOT_FOUND");
    assertThat(body.get("path").asText()).isEqualTo("/api/v1/definitely-not-a-route");
  }

  @Test
  void wrongMethod_returnsMethodNotAllowedWithAllowHeader() throws Exception {
    MvcResult result = mockMvc.perform(delete("/test-errors/event-not-found"))
        .andExpect(status().isMethodNotAllowed())
        .andReturn();

    assertThat(bodyOf(result).get("code").asText()).isEqualTo("METHOD_NOT_ALLOWED");
    assertThat(result.getResponse().getHeader("Allow")).contains("GET");
  }

  @Test
  void unsupportedMediaType_returns415() throws Exception {
    MvcResult result = mockMvc.perform(post("/test-errors/validated")
            .contentType(MediaType.TEXT_PLAIN)
            .content("plain text"))
        .andExpect(status().isUnsupportedMediaType())
        .andReturn();

    assertThat(bodyOf(result).get("code").asText()).isEqualTo("UNSUPPORTED_MEDIA_TYPE");
  }

  @Test
  void malformedJson_returns400WithoutParserInternals() throws Exception {
    MvcResult result = mockMvc.perform(post("/test-errors/validated")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{bad json"))
        .andExpect(status().isBadRequest())
        .andReturn();

    JsonNode body = bodyOf(result);
    assertThat(body.get("code").asText()).isEqualTo("BAD_REQUEST");
    assertThat(body.get("error").asText())
        .doesNotContain("Unexpected character")
        .doesNotContain("JsonParseException")
        .doesNotContain("com.fasterxml");
  }

  @Test
  void missingRequestBody_returns400() throws Exception {
    MvcResult result = mockMvc.perform(post("/test-errors/validated")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andReturn();

    assertThat(bodyOf(result).get("code").asText()).isEqualTo("BAD_REQUEST");
  }

  @Test
  void missingRequiredRequestParameter_returns400() throws Exception {
    MvcResult result = mockMvc.perform(get("/test-errors/param"))
        .andExpect(status().isBadRequest())
        .andReturn();

    assertThat(bodyOf(result).get("code").asText()).isEqualTo("BAD_REQUEST");
  }

  @Test
  void wrongTypedRequestParameter_returns400() throws Exception {
    MvcResult result = mockMvc.perform(get("/test-errors/param").param("count", "abc"))
        .andExpect(status().isBadRequest())
        .andReturn();

    assertThat(bodyOf(result).get("code").asText()).isEqualTo("BAD_REQUEST");
  }

  // --- B13-B15: Bean Validation ---

  private Set<String> fieldPairs(JsonNode body) {
    Set<String> pairs = new HashSet<>();
    body.get("fields").forEach(field -> pairs.add(field.get("field").asText()));
    return pairs;
  }

  @Test
  void validation_withEveryFieldInvalid_listsEveryField() throws Exception {
    MvcResult result = mockMvc.perform(post("/test-errors/validated")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\": \"\", \"age\": 0}"))
        .andExpect(status().isBadRequest())
        .andReturn();

    JsonNode body = bodyOf(result);
    assertThat(body.get("code").asText()).isEqualTo("VALIDATION_FAILED");
    assertThat(keysOf(body)).containsExactlyInAnyOrder(
        "error", "code", "timestamp", "path", "fields");
    assertThat(fieldPairs(body)).containsExactlyInAnyOrder("name", "age");
    body.get("fields").forEach(field -> assertThat(field.get("message").asText()).isNotBlank());
  }

  @Test
  void validation_withOneFieldInvalid_listsOnlyThatField() throws Exception {
    MvcResult result = mockMvc.perform(post("/test-errors/validated")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\": \"alice\", \"age\": 0}"))
        .andExpect(status().isBadRequest())
        .andReturn();

    assertThat(fieldPairs(bodyOf(result))).containsExactly("age");
  }

  @Test
  void validation_withValidBody_isNotInterferedWith() throws Exception {
    MvcResult result = mockMvc.perform(post("/test-errors/validated")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\": \"alice\", \"age\": 3}"))
        .andExpect(status().isOk())
        .andReturn();

    assertThat(result.getResponse().getContentAsString()).isEqualTo("alice");
  }

  // --- B16: errors are always JSON, whatever the Accept header says ---

  @ParameterizedTest(name = "Accept: {0}")
  @CsvSource({"application/xml", "text/html", "text/plain"})
  void errorResponse_isJsonEvenWhenClientDoesNotAcceptJson(String accept) throws Exception {
    MvcResult result = mockMvc.perform(get("/test-errors/event-not-found").accept(accept))
        .andExpect(status().isNotFound())
        .andReturn();

    assertThat(result.getResponse().getContentType()).startsWith("application/json");
    assertThat(bodyOf(result).get("code").asText()).isEqualTo("EVENT_NOT_FOUND");
  }

  @Test
  void unknownRoute_isJsonEvenWhenClientDoesNotAcceptJson() throws Exception {
    MvcResult result = mockMvc.perform(
            get("/api/v1/definitely-not-a-route").accept(MediaType.APPLICATION_XML))
        .andExpect(status().isNotFound())
        .andReturn();

    assertThat(result.getResponse().getContentType()).startsWith("application/json");
    assertThat(bodyOf(result).get("code").asText()).isEqualTo("ROUTE_NOT_FOUND");
  }

  @Test
  void unexpectedError_isJsonEvenWhenClientDoesNotAcceptJson() throws Exception {
    MvcResult result = mockMvc.perform(
            get("/test-errors/unexpected").accept(MediaType.APPLICATION_XML))
        .andExpect(status().isInternalServerError())
        .andReturn();

    assertThat(result.getResponse().getContentType()).startsWith("application/json");
    assertThat(bodyOf(result).get("code").asText()).isEqualTo("INTERNAL_ERROR");
  }
}
