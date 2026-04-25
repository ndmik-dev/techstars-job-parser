package io.ndmik.tsparser.controller;

import io.ndmik.tsparser.service.JobNotFoundException;
import io.ndmik.tsparser.service.ScrapeAlreadyRunningException;
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

    @ExceptionHandler(ScrapeAlreadyRunningException.class)
    ProblemDetail handleScrapeAlreadyRunning(ScrapeAlreadyRunningException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle("Scrape already running");
        problemDetail.setDetail(exception.getMessage());
        return problemDetail;
    }
}
