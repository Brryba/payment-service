package innowise.payments_service.exception;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StatusCodeException extends RuntimeException {
    protected final int statusCode;
    protected final String message;
}
