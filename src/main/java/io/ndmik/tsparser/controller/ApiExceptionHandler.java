package io.ndmik.tsparser.controller;

import io.ndmik.tsparser.service.JobNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(JobNotFoundException.class)
    ProblemDetail handleJobNotFound(JobNotFoundException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Job not found");
        problemDetail.setDetail(exception.getMessage());
        return problemDetail;
    }
}
