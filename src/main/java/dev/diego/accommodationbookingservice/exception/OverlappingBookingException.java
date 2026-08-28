package dev.diego.accommodationbookingservice.exception;

public class OverlappingBookingException extends RuntimeException {
    public OverlappingBookingException(String message) {
        super(message);
    }
}
