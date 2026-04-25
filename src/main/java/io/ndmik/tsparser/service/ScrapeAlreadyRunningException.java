package io.ndmik.tsparser.service;

public class ScrapeAlreadyRunningException extends RuntimeException {

    public ScrapeAlreadyRunningException() {
        super("Scrape is already running");
    }
}
