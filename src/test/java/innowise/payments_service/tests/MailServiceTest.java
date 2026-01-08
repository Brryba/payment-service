package innowise.payments_service.tests;

import com.google.auth.oauth2.UserCredentials;
import innowise.payments_service.config.GoogleCredentialsConfig;
import innowise.payments_service.service.EmailSenderService;
import innowise.payments_service.service.GmailTokenProvider;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
        EmailSenderService.class,
        GmailTokenProvider.class,
        GoogleCredentialsConfig.class
})
@Disabled
@ImportAutoConfiguration(MailSenderAutoConfiguration.class)
public class MailServiceTest {
    @Autowired
    private EmailSenderService gmailSenderService;

    @Test
    public void sendEmail() {
        gmailSenderService.sendEmail();
    }
}
