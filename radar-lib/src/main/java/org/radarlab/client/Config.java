package org.radarlab.client;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * Created by Andy
 * since 14-10-11.
 */
public class Config {
    private static final Logger logger = Logger.getLogger(Config.class);
    Properties config = new Properties();
    private static Config instance = new Config();
    private static String xml;



    public Config() {
        synchronized (Config.class) {
            loadProperties();
            loadXML();
        }
    }

    private void loadProperties() {
        String[] fileNames = new String[]{"config.properties"};
        for (String fileName : fileNames) {
            InputStream in;
            in = getClass().getResourceAsStream("/" + fileName);
            try {
                if (in != null)
                    config.load(in);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadXML(){
        logger.debug("init xml config file..");
        try{
            String ws = "/websocket.xml";
            InputStream in = getClass().getResourceAsStream(ws);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in,"utf-8"));
            xml = new String();
            String l;
            while ((l = reader.readLine())!=null){
                xml+=l;
            }
        }catch(Exception e){
            logger.info("Read exclude xml file error, message is ", e);
        }
    }

    public static Config getInstance() {
        return instance;
    }

    public String getProperty(String key) {
        return config.getProperty(key);
    }

    public String getServer(String type) {
        try{
            Document d = DocumentHelper.parseText(xml);
            Element root = d.getRootElement();
            Element auth = root.element(type);
            if(auth != null){
                return auth.getText();
            }else
                return null;
        }catch(Exception e){
            logger.error("get excludes tag error. message is "+e.getMessage());
            return null;
        }
    }


}
