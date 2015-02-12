package org.radarlab.core.exception;

/**
 * Exception of all radarj
 */
public class RadarException extends RuntimeException {
    private int code;
    private String msg;


    public RadarException(String msg){
        super(msg);
        this.msg = msg;
    }

    public RadarException(int code, String msg){
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
