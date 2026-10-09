package york.studentevents.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Structural guard for the rule that error handling lives only in {@link ApiExceptionHandler}:
 * no controller may declare its own {@code @ExceptionHandler}, as a controller-local handler takes
 * precedence over the advice and would bypass the standard error format.
 */
class NoControllerErrorHandlingTest {

  private static List<Class<?>> findControllers() throws ClassNotFoundException {
    var scanner = new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));

    List<Class<?>> controllers = new ArrayList<>();
    for (BeanDefinition definition : scanner.findCandidateComponents("york.studentevents")) {
      controllers.add(Class.forName(definition.getBeanClassName()));
    }
    return controllers;
  }

  @Test
  void scan_findsTheApplicationsControllers() throws ClassNotFoundException {
    // Guards against the main test passing vacuously because nothing was scanned
    assertThat(findControllers()).isNotEmpty();
  }

  @Test
  void noControllerDeclaresItsOwnExceptionHandler() throws ClassNotFoundException {
    List<String> offenders = new ArrayList<>();

    for (Class<?> controller : findControllers()) {
      for (Method method : controller.getMethods()) {
        if (method.isAnnotationPresent(ExceptionHandler.class)) {
          offenders.add(controller.getSimpleName() + "#" + method.getName());
        }
      }
      for (Method method : controller.getDeclaredMethods()) {
        if (method.isAnnotationPresent(ExceptionHandler.class)) {
          offenders.add(controller.getSimpleName() + "#" + method.getName());
        }
      }
    }

    assertThat(offenders)
        .as("Controllers must not handle exceptions themselves; use ApiExceptionHandler")
        .isEmpty();
  }
}
