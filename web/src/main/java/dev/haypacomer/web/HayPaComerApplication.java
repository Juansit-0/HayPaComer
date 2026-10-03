package dev.haypacomer.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "dev.haypacomer")
@ConfigurationPropertiesScan
@EnableScheduling
public class HayPaComerApplication {

  public static void main(String[] args) {
    SpringApplication.run(HayPaComerApplication.class, args);
  }
}
