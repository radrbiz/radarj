package org.radarlab.client.wsserver;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AccountDividendThread implements Runnable{

    private static final ConcurrentMap<String, Map> dividend = new ConcurrentHashMap<>();
    private static final Logger logger = Logger.getLogger(AccountDividendThread.class);
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");



    @Override
    public void run() {
        long start = System.currentTimeMillis();
        String date = format.format(new Date());
        String fileName = "/log/log." + date;
        logger.info("start to check local data file:" + fileName);
//        String fileName = "/Users/wenfeng/Documents/log." + date;
        try {
            if (dividend.get(date) == null) {
                File log = new File(fileName);
                if (log.exists()) {
                    FileInputStream in = new FileInputStream(log);
                    InputStreamReader reader = new InputStreamReader(in);
                    BufferedReader bufferedReader = new BufferedReader(reader);
                    String line;
                    Map<String, String> dividendData = new ConcurrentHashMap<>();
                    while ((line=bufferedReader.readLine())!=null){
                        try {
                            line = line.split("DividendMaster:NFO")[1];
                            JSONObject json = new JSONObject(line);
                            String account = json.getString("account");
                            String data = json.getJSONObject("data").toString();
                            dividendData.putIfAbsent(account, data);
                        }catch (Exception e){
                            logger.error("error when adding data to map;");
                            e.printStackTrace();
                            continue;
                        }
                    }
                    dividend.clear();
                    dividend.put("data", dividendData);
                    long end = System.currentTimeMillis();
                    logger.info("file loaded, time:" + (end-start));
                }else{
                    logger.info("data is expired, but the log file has not been found.");
                }
            }else{
                logger.info("local file has already loaded.");
            }
        }catch (Exception ex){
            ex.printStackTrace();
            logger.info("error to read from log file:" + fileName);
        }
    }

    public static Map<String, String> getDividendMap() {
        return Collections.unmodifiableMap(dividend.get("data"));
    }
}
