package innowise.payments_service.config;

import com.google.auth.oauth2.UserCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleCredentialsConfig {
    @Value("${spring.mail.gmail.refresh-token}")
    private String refreshToken;

    @Value("${spring.mail.gmail.client-id}")
    private String clientId;

    @Value("${spring.mail.gmail.client-secret}")
    private String clientSecret;

    @Bean
    public UserCredentials googleCredentials() {
        return UserCredentials.newBuilder()
                .setRefreshToken(refreshToken)
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .build();
    }
}
