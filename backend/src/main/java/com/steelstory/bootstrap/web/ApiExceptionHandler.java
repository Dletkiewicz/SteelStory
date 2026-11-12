package com.steelstory.bootstrap.web;

import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> unexpected(Exception exception, WebRequest request) {
        // JDBC/parser exception messages can include credentials or a rejected row.
        LOG.error("Unhandled API failure ({})", exception.getClass().getSimpleName());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.");
        problem.setType(URI.create("urn:steelstory:problem:internal-error"));
        problem.setProperty("code", "INTERNAL_ERROR");
        return handleExceptionInternal(exception, problem, new org.springframework.http.HttpHeaders(),
                HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
}
