package york.studentevents.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import york.studentevents.exceptions.EntityNotFoundException;

class ApiEntityNotFoundExceptionHandlerCoverageTest {

  private final ApiExceptionHandler handler = new ApiExceptionHandler();

  static Stream<Class<? extends EntityNotFoundException>> notFoundSubclasses() {
    var scanner = new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AssignableTypeFilter(EntityNotFoundException.class));

    return scanner.findCandidateComponents("york.studentevents.exceptions").stream()
        .map(BeanDefinition::getBeanClassName)
        .map(ApiEntityNotFoundExceptionHandlerCoverageTest::load)
        .filter(type -> type != EntityNotFoundException.class)
        .map(type -> type.asSubclass(EntityNotFoundException.class));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("notFoundSubclasses")
  @DisplayName("every EntityNotFoundException subclass is mapped by the handler")
  void everySubclassIsMapped(Class<? extends EntityNotFoundException> type) {
    EntityNotFoundException ex = mock(type);

    // Throws IllegalStateException if the handler does not know this subclass
    ResponseEntity<ApiErrorResponse> response =
        handler.handleEntityNotFoundException(ex, new MockHttpServletRequest());

    assertThat(response.getStatusCode().value()).isEqualTo(404);
    assertThat(response.getBody().code()).isNotNull();
  }

  private static Class<?> load(String name) {
    try {
      return Class.forName(name);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }
}