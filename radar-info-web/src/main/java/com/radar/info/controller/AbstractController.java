package com.radar.info.controller;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.radarlab.client.Config;
import org.radarlab.core.exception.RadarException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.HashMap;
import java.util.Map;

public class AbstractController {
    protected static final String HTTP_URI = Config.getInstance().getProperty("http.server.backend");
    private static final Logger logger = Logger.getLogger(AbstractController.class);

    /**
     * HANDEL APIEXCEPTION
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(RadarException.class)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String handHttpMethodNotSupportedException(RadarException ex) {
        ex.printStackTrace();
        logger.error("Exception handler.", ex);
        Map<String, Object> data = new HashMap<>();
        data.put("error_code", ex.getCode());
        data.put("message", ex.getMsg());
        Map<String, Object> result = new HashMap<>();
        result.put("status", "failed");
        result.put("data", data);
        return new Gson().toJson(result);
    }

    /**
     * Handle other exception
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String handleException(Exception ex) {
        ex.printStackTrace();
        logger.error("Exception handler.", ex);
        Map<String, Object> data = new HashMap<>();
        data.put("error_code", "Internal Error.");
        //data.put("message", ex.getMessage());
        data.put("message", "internal error,try again later");
        Map<String, Object> result = new HashMap<>();
        result.put("status", "failed");
        result.put("data", data);
        return new Gson().toJson(result);
    }


}
