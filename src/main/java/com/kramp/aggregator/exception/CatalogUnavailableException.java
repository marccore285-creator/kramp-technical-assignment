package com.kramp.aggregator.exception;

/** Thrown when the Catalog service fails or times out. Maps to HTTP 503. */
public class CatalogUnavailableException extends RuntimeException {

    public CatalogUnavailableException(String message) {
        super(message);
    }

    public CatalogUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
