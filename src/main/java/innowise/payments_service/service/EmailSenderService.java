package innowise.payments_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailSenderService {
    private final JavaMailSender mailSender;
    private final GmailTokenProvider gmailTokenProvider;

    public void sendEmail() {
        String accessToken = gmailTokenProvider.getAccessToken();
    }
}
