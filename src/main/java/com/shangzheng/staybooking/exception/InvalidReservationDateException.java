package com.shangzheng.staybooking.exception;

public class InvalidReservationDateException extends RuntimeException{
    public InvalidReservationDateException(String message) {
        super(message);
    }

}
