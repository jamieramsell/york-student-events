package york.studentevents.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebConfig is a configuration class that implements the WebMvcConfigurer interface to customise
 * the configuration of Spring Web MVC.
 *
 * <p>This class specifically enables and configures Cross-Origin Resource Sharing (CORS) for the
 *     application. The allowed origins for CORS requests can be set through the
 *     `cors.allowed-origins` property.
 *
 * <p>The addCorsMappings method is overridden to define CORS mapping rules for the application,
 *     allowing specific HTTP methods and headers for cross-origin requests.
 */
public class WebConfig implements WebMvcConfigurer {
  @Value("${cors.allowed-origins}")
  private String[] allowedOrigins;

  /**
   * Configures Cross-Origin Resource Sharing (CORS) for the application.
   *
   * <p>This method defines CORS mapping rules, specifying the allowed origins, HTTP methods, and
   *     headers for cross-origin requests. It applies the mapping to all endpoints within the
   *     application.
   *
   * @param registry the CorsRegistry object used to configure CORS mappings; must not be null.
   */
  @Override
  public void addCorsMappings(@NonNull CorsRegistry registry) {
    registry.addMapping("/**")
        .allowedOrigins(allowedOrigins)
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*");
  }
}




  