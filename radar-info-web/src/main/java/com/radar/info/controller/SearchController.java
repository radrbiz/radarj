package com.radar.info.controller;

import com.google.gson.Gson;
import com.radar.util.HttpClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.radarlab.core.exception.RadarException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping(value = "/search", produces = "application/json;charset=UTF-8")
public class SearchController extends AbstractController {

    @RequestMapping(value = "/{address}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String accountInfo(
            @RequestParam(value = "type", required = false, defaultValue = "info" )String type,
            @RequestParam(value = "full", required = false )String full,
            @PathVariable("address")String address
    ) throws RadarException {
        if(StringUtils.isBlank(address)){
            throw new RadarException("Invalid parameters.");
        }
        String resp;
        if(address.startsWith("r")){
            Map<String, Object> params = new HashMap<>();
            Map<String, Object> para = new HashMap<>();
            para.put("account", address);
            para.put("strict", true);
            para.put("ledger_index", "validated");
            if("info".equalsIgnoreCase(type)) {
                params.put("method", "account_info");
            }else if("lines".equalsIgnoreCase(type)){
                params.put("method", "account_lines");
            }else if("offers".equalsIgnoreCase(type)){
                params.put("method", "account_offers");
            }else if("tx".equalsIgnoreCase(type)){
                params.put("method", "account_tx");
                para.put("limit", 20);
            }
            params.put("params", Collections.singletonList(para));
            resp = HttpClient.post(HTTP_URI, new Gson().toJson(params)).getResponseString();
        }else if(NumberUtils.isDigits(address)){
            Map<String, Object> params = new HashMap<>();
            Map<String, Object> para = new HashMap<>();
            para.put("ledger_index", Long.valueOf(address));
            para.put("accounts", false);
            para.put("expand", StringUtils.isNotEmpty(full));
            para.put("full", false);
            para.put("transactions", true);
            params.put("method", "ledger");
            params.put("params", Collections.singletonList(para));
            resp = HttpClient.post(HTTP_URI, new Gson().toJson(params)).getResponseString();
        }else if(address.length() > 50){
            Map<String, Object> params = new HashMap<>();
            Map<String, Object> para = new HashMap<>();
            para.put("transaction", address);
            para.put("binary", false);
            params.put("method", "tx");
            params.put("params", Collections.singletonList(para));
            resp = HttpClient.post(HTTP_URI, new Gson().toJson(params)).getResponseString();
        }else{
            throw new RadarException("Invalid parameters.");
        }
        return resp;
    }
}
