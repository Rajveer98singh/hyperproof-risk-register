package com.hyperproof.riskregister.error;

/** Thrown when a request violates a domain rule (e.g. closing a risk with no mitigations). HTTP 409. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
