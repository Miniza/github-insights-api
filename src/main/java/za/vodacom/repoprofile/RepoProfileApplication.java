package za.vodacom.repoprofile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RepoProfileApplication {

  public static void main(String[] args) {
    SpringApplication.run(RepoProfileApplication.class, args);
  }

}
