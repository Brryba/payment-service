package innowise.payments_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailSenderService {
    private final JavaMailSender mailSender;
    private final GmailTokenProvider gmailTokenProvider;

    public void sendEmail() {
        String accessToken;
        try {
            accessToken = gmailTokenProvider.getAccessToken();
        } catch (IOException e) {
            log.error("Error updating access token: {}", e.getMessage());
            return;
        }
    }
}
