package innowise.payments_service.service;

import com.google.auth.oauth2.UserCredentials;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class GmailTokenProvider {
    private static final int SAFETY_MARGIN_SECONDS = 15;

    private final UserCredentials googleCredentials;

    @PostConstruct
    public void initializeGoogleUserCredentials() {
        try {
            googleCredentials.refresh();
            log.info("Gmail access token was successfully received");
        } catch (IOException e) {
            log.error("Error initializing google credentials: {}", e.getMessage());
        }
    }

    public String getAccessToken() throws IOException {
        if (isTokenExpiresSoon()) {
            log.info("Gmail access token is expired. Requesting new one...");
            googleCredentials.refresh();
            log.info("Gmail access token was successfully updated");
        }

        return googleCredentials.getAccessToken().getTokenValue();
    }

    private boolean isTokenExpiresSoon() {
        if (googleCredentials.getAccessToken() == null) {
            return true;
        }

        Instant expirationTime = googleCredentials.getAccessToken().getExpirationTime().toInstant();
        return Instant.now().isAfter(expirationTime.minusSeconds(SAFETY_MARGIN_SECONDS));
    }
}
