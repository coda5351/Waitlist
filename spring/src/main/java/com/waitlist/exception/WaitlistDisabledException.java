package com.waitlist.exception;

/**
 * Thrown when an operation would touch the waitlist while it is disabled.
 */
public class WaitlistDisabledException extends RuntimeException {
    public WaitlistDisabledException() {
        super("Waitlist is disabled");
    }

    public WaitlistDisabledException(String message) {
        super(message);
    }
}
