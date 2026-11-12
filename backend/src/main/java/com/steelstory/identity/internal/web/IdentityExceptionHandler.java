package com.steelstory.identity.internal.web;

import com.steelstory.identity.internal.application.EmailAlreadyRegisteredException;
import com.steelstory.identity.internal.application.InvalidCredentialsException;
import com.steelstory.identity.internal.domain.InvalidIdentityInputException;
import java.net.URI;
import java.util.Locale;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(-1)
@RestControllerAdvice(assignableTypes = AuthController.class)
public class IdentityExceptionHandler {
    @ExceptionHandler(InvalidIdentityInputException.class)
    ProblemDetail invalidInput(InvalidIdentityInputException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_INPUT", exception.getMessage());
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    ProblemDetail duplicateEmail() {
        return problem(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", EmailAlreadyRegisteredException.MESSAGE);
    }

    @ExceptionHandler({InvalidCredentialsException.class, AuthenticationException.class})
    ProblemDetail invalidCredentials() {
        return problem(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", InvalidCredentialsException.MESSAGE);
    }

    private ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("urn:steelstory:problem:" + code.toLowerCase(Locale.ROOT)
                .replace('_', '-')));
        problem.setProperty("code", code);

        return problem;
    }
}
