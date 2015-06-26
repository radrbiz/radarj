package org.radarlab.client.api.exception;

/**
 * Created by Andy
 * since 14/12/19.
 */
public class APIException extends Exception{

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
        USER_HAS_SET_PAYPASSWORD,
        MOBILE_VERIFY_FAILED,
        USER_HAS_NOT_BIND_MOBILE,
        USER_ACCOUNT_BALANCE_NOT_ENOUGH,
        NOT_FOUND,
        INCORRECT_PHONE_CODE,
        PHONE_CODE_TOO_FREQUENCY,
        SPREAD_TOKEN_NOT_FOUND,
        SPREAD_TOKEN_INVALID,
        SPREAD_TOKEN_HAS_BEENUSERD,
        OPENUSER_DATA_FORMAT_ERROR,
        OPENUSER_USER_NOT_FOUND,
        OPEN_ORDER_NOT_FOUND,
        ORDER_HAS_BEEN_PAYED,
        OPEN_SIGN_INVALID,
        EMIAL_TOO_FREQUENCY,
        INVALID_URL_TOKEN,
        PAY_PASSWORD_MATCH_LOGIN,
        USER_MOBILE_FORMAT_ERROR,
        SEED_VERIFY_FAILED,
        RESET_PASSWORD_ERROR,
        LOGIN_TIMES_LIMIT,
        REGISTER_TIMES_LIMIT,
        WS_IN_CONNECTING_MODE,
        RESET_PASSWORD_TOKEN_INVLID,
        REFEREE_NOT_FOUND,
        ADDREF_TOKEN_NOT_FOUND,
        OPEN_USER_UNACTIVATED,
        SPREAD_TOKEN_CAN_NOT_CANCELD,
        INVALID_REFEREE_WITH_NO_REFER,
        INVALID_ACCOUNT_WITH_NO_REFER,
        ASSET_NOT_FOUND,
        USER_HAS_NOT_SET_GOOGLEAUTH,
        USER_HAS_SET_PHONE_VERIFY,
        USER_HAS_SET_GOOGLE_VERIFY,
        INCORRECT_GOOGLE_VERIFY_CODE,
        PHONE_HAS_EXIST,
        INVALID_PARAMS,
        OPERATION_TOO_FREQUENCY,
        TOKEN_NOT_FOUND,
        REMOTE_SERVER_BUSY
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
