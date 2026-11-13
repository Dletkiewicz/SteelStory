package com.steelstory.character.internal.web;

import com.steelstory.character.internal.application.CharacterLimitReachedException;
import com.steelstory.character.internal.application.CharacterNameTakenException;
import com.steelstory.character.internal.application.CharacterNotFoundException;
import com.steelstory.character.internal.application.InactiveAccountException;
import com.steelstory.character.internal.domain.InvalidCharacterInputException;
import java.net.URI;
import java.util.Locale;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(-1)
@RestControllerAdvice(assignableTypes = {CharacterController.class, DevelopmentExperienceController.class})
public class CharacterExceptionHandler {
    @ExceptionHandler(InvalidCharacterInputException.class)
    ProblemDetail invalidInput(InvalidCharacterInputException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_CHARACTER_INPUT", exception.getMessage());
    }

    @ExceptionHandler(CharacterNameTakenException.class)
    ProblemDetail nameTaken(CharacterNameTakenException exception) {
        return problem(HttpStatus.CONFLICT, "CHARACTER_NAME_TAKEN", exception.getMessage());
    }

    @ExceptionHandler(CharacterLimitReachedException.class)
    ProblemDetail limitReached(CharacterLimitReachedException exception) {
        return problem(HttpStatus.CONFLICT, "CHARACTER_LIMIT_REACHED", exception.getMessage());
    }

    @ExceptionHandler(CharacterNotFoundException.class)
    ProblemDetail notFound(CharacterNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "CHARACTER_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(InactiveAccountException.class)
    ProblemDetail inactiveAccount(InactiveAccountException exception) {
        return problem(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", exception.getMessage());
    }

    private ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("urn:steelstory:problem:" + code.toLowerCase(Locale.ROOT).replace('_', '-')));
        problem.setProperty("code", code);

        return problem;
    }
}
