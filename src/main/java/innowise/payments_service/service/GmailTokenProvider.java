package innowise.payments_service.service;

import innowise.payments_service.client.GoogleAuthClient;
import innowise.payments_service.dto.google_oauth2.GoogleAccessTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class GmailTokenProvider {
    private final GoogleAuthClient googleAuthClient;

    @Value("${spring.mail.gmail.refresh-token}")
    private String refreshToken;

    @Value("${spring.mail.gmail.client-id}")
    private String clientId;

    @Value("${spring.mail.gmail.client-secret}")
    private String clientSecret;

    private final int SAFETY_MARGIN_SECONDS = 2;

    private volatile String accessToken;
    private volatile Instant expiresAt;

    public String getAccessToken() {
        if (isTokenExpired()) {
            synchronized (this) {
                if (isTokenExpired()) {
                    GoogleAccessTokenResponse response = googleAuthClient.refreshAccessToken(
                            clientId, clientSecret, refreshToken, "refresh_token"
                    );
                    accessToken = response.getAccessToken();
                    expiresAt = Instant.now().plusSeconds(response.getExpiresIn());
                }
            }
        }

        return accessToken;
    }

    private boolean isTokenExpired() {
        return accessToken == null || Instant.now().isAfter(expiresAt.minusSeconds(SAFETY_MARGIN_SECONDS));
    }
}
