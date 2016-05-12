package com.radar.info.controller;

import com.radar.info.job.LedgersWatcher;
import org.json.JSONArray;
import org.json.JSONObject;
import org.radarlab.core.exception.RadarException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

@Controller
@RequestMapping(value = "/ledger", produces = "application/json;charset=UTF-8")
public class LedgerController extends AbstractController {

    @RequestMapping(value = "/recent", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String accountInfo() throws RadarException {
        ConcurrentMap<Integer, String> ledgers = LedgersWatcher.ledgers;
        Integer[] ks = new Integer[ledgers.keySet().size()];
        ledgers.keySet().toArray(ks);
        List<Integer> keys = Arrays.asList(ks);
        ledgers.keySet().toArray(ks);
        Collections.sort(keys, (o1, o2) -> o2 - o1);
        JSONArray list = new JSONArray();
        keys.forEach(key -> {
            if (list.length() <= 50 && ledgers.get(key) != null)
                list.put(new JSONObject(ledgers.get(key)));
        });
        return list.toString();
    }
}
