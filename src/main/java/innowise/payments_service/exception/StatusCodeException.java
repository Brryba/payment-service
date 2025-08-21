package innowise.payments_service.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public class StatusCodeException extends RuntimeException {
    protected final HttpStatus statusCode;
    protected final String message;
}
