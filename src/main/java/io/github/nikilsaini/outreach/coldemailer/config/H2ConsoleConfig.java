package io.github.nikilsaini.outreach.coldemailer.config;

import org.h2.server.web.JakartaWebServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class H2ConsoleConfig {

  @Bean
  public ServletRegistrationBean<JakartaWebServlet> h2ConsoleServlet() {
    ServletRegistrationBean<JakartaWebServlet> registration =
        new ServletRegistrationBean<>(new JakartaWebServlet(), "/h2-console/*");
    registration.addInitParameter("webAllowOthers", "true");
    return registration;
  }
}
