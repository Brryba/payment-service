package innowise.payments_service.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
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

        try {
            JavaMailSenderImpl sender = (JavaMailSenderImpl) mailSender;

            sender.setPassword(accessToken);

            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setTo("jurabrezgunov@gmail.com");
            helper.setSubject("Welcome");
            helper.setText("Is it working?", false);

            sender.send(message);
            log.info("Email sent ");

        } catch (Exception e) {
            log.error("Failed to send email", e);
        }
    }
}
