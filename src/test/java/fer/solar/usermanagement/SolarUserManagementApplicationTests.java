package fer.solar.usermanagement;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "auth0.domain=test.domain.com",
    "auth0.management.clientId=testclientid",
    "auth0.management.clientSecret=testclientsecret",
    "auth0.management.apiAudience=testaudience",
    "spring.scheduling.enabled=false"
})
@Import(TestConfig.class)
class SolarUserManagementApplicationTests {

	@Test
	void contextLoads() {
	}

}
