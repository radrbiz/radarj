package org.radarlab.api;


public class APIException extends RuntimeException{

    public static enum ErrorCode{
        INTERNAL_ERROR,
        USER_NOT_FOUND,
        UNKNOWN_ERROR,
        ADDRESS_NOT_FOUND,
        ADDRESS_FORMAT_MALFORMED,
        REMOTE_ERROR,
        MALFORMED_REQUEST_DATA,
        UNSUPPORTED_CURRENCY,
        NOT_LOGGEDIN,
        INCORRECT_PASSWORD,
        NICK_EXISTS,
        NOT_RECEIVED_CURRENCY,
        NOT_ENOUGH_BALANCE,
        ACCOUNT_LOCK,
        IS_GATEWAY,
        ACTIVATED_ERROR,
        SENDMAIL_ERROR,
        EMIAL_EXISTS,
        MASTERKEY_EXISTS,
        MASTERKEY_FORMAT_ERROR,
        NICK_INVALID,
        TOO_MANY_REQUESTS,
        PAY_PASSWORD_SET_FAILED,
        USER_NOT_SET_PAYPASSWORD,
        PAYPASSWORD_ERROR,
        USER_HAS_SET_PAYPASSWORD
    }

    public ErrorCode code;
    public String message;

    public APIException(ErrorCode code, String message){
        super(message);
        this.code = code;
        this.message = message;
    }

    public APIException(String message){
        super(message);
        this.message = message;
    }
}
