package com.edutrust.generation;

/** Indicates that the local Ollama answer service could not complete a request. */
public class AnswerServiceUnavailableException extends RuntimeException {

    public AnswerServiceUnavailableException(Throwable cause) {
        super("The answer service is not available", cause);
    }
}
