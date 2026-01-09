package innowise.payments_service.service;

import innowise.payments_service.client.UserServiceClient;
import innowise.payments_service.entity.Payment;
import innowise.payments_service.entity.Status;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailSenderService {
    private final JavaMailSender mailSender;
    private final GmailTokenProvider gmailTokenProvider;
    private final UserServiceClient userServiceClient;

    @Async
    public void sendEmail(Long userId, Payment payment) {
        String accessToken;
        try {
            accessToken = gmailTokenProvider.getAccessToken();
        } catch (IOException e) {
            log.error("Error updating access token: {}", e.getMessage());
            return;
        }

        String userEmail;
        try {
            log.info("Requesting user {} id from user service", userId);

            userEmail = userServiceClient.getUserById(userId).getEmail();

            log.debug("Received email for user {}: {}", userId, userEmail);
        } catch (Exception e) {
            log.error("Failed to receive user email: {}", e.getMessage());
            return;
        }

        try {
            JavaMailSenderImpl sender = (JavaMailSenderImpl) mailSender;

            sender.setPassword(accessToken);

            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setTo(userEmail);
            setMailData(payment, helper);
            sender.send(message);
            log.info("Email was sent to user {}", userEmail);

        } catch (Exception e) {
            log.error("Error sending email to {}: ", userEmail, e);
        }
    }

    private void setMailData(Payment payment, MimeMessageHelper helper) throws MessagingException {
        helper.setSubject("Your payment was " + (payment.getStatus().equals(Status.COMPLETED) ? "completed" : "failed"));
        if (payment.getStatus().equals(Status.COMPLETED)) {
            helper.setText("Your payment for order " + payment.getOrderId() + " was successfully completed! \n" +
                    payment.getPaymentAmount() + "$ were debited from your card. \n" +
                    "Your order will be delivered as soon as possible. \n\n" +
                    "Thank you for using our store!");
        } else if (payment.getStatus().equals(Status.FAILED)) {
            helper.setText("Unfortunately your payment for order " + payment.getOrderId() + " has failed."
            + " Please try again later or use another payment method.");
        }
    }
}
