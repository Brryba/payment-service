package innowise.payments_service.client;

import innowise.payments_service.dto.google_oauth2.GoogleAccessTokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "google-auth", url = "https://oauth2.googleapis.com")
public interface GoogleAuthClient {
    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    GoogleAccessTokenResponse refreshAccessToken(
            @RequestParam("client_id") String clientId,
            @RequestParam("client_secret") String clientSecret,
            @RequestParam("refresh_token") String refreshToken,
            @RequestParam("grant_type") String grantType
    );
}