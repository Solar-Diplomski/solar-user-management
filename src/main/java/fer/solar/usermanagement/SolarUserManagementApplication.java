package fer.solar.usermanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// Explicitly scan the new base package if needed, though often automatic
// @ComponentScan(basePackages = "fer.solar.usermanagement")
public class SolarUserManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(SolarUserManagementApplication.class, args);
	}

} 