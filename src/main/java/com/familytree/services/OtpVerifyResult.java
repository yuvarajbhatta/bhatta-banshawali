package com.familytree.services;

public enum OtpVerifyResult {
    OK,
    NOT_FOUND,
    EXPIRED,
    TOO_MANY_ATTEMPTS,
    INVALID_CODE
}
