package york.studentevents.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * HealthController is a REST controller that provides an endpoint to check the health status
 * of the application.
 * <p>
 * This controller handles HTTP GET requests to the /health endpoint, responding with a
 * JSON object indicating the application's health status.
 */
@RestController
@RequestMapping("${api.base-path}")
public class HealthController {

  /**
   * Handles HTTP GET requests to the "/health" endpoint.
   * <p>
   * This method returns a JSON string indicating the current health status of the application.
   *
   * @return a JSON-formatted string containing the health status of the application.
   */
  @GetMapping("/health")
  public String health() {
    return "{\n  \"status\": \"OK\"\n}";
  }
}
