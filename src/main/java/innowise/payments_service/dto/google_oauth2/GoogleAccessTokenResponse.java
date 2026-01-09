package innowise.payments_service.dto.google_oauth2;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GoogleAccessTokenResponse {
    @JsonProperty("access_token") String accessToken;
    @JsonProperty("expires_in") int expiresIn;
}