package fer.solar.usermanagement;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import fer.solar.usermanagement.config.Auth0Config;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    Auth0Config auth0Config() {
        return Mockito.mock(Auth0Config.class);
    }
} 