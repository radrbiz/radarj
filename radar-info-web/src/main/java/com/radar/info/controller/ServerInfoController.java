package com.radar.info.controller;

import com.google.gson.Gson;
import com.radar.util.HttpClient;
import org.radarlab.core.exception.RadarException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping(value = "/server", produces = "application/json;charset=UTF-8")
public class ServerInfoController extends AbstractController{
    @RequestMapping(value = "/{type}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String accountInfo(
            @PathVariable("type")String type
    ) throws RadarException {
        Map<String, Object> params = new HashMap<>();
        if("info".equalsIgnoreCase(type))
            params.put("method", "server_info");
        else
            params.put("method", "peers");
        String resp = HttpClient.post(HTTP_URI, new Gson().toJson(params)).getResponseString();
        return resp;
    }
}
