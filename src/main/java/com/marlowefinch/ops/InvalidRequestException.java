package com.marlowefinch.ops;

import java.util.List;

/** Thrown when request parameters fail validation; carries every problem found. Mapped to 400 by {@link ApiExceptionHandler}. */
public class InvalidRequestException extends RuntimeException {

    private final List<String> errors;

    public InvalidRequestException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> errors() {
        return errors;
    }
}
