package org.radarlab.client;

import java.io.*;
import java.util.Properties;

/**
 */
public class Config {
    Properties config = new Properties();
    private static Config instance = new Config();

    private Config() {
        synchronized (Config.class) {
            loadProperties();
        }
    }

    private void loadProperties() {
        String[] fileNames = new String[]{"radar.properties", "radar.properties"};
        for (String fileName : fileNames) {
            InputStream in;
            in = getClass().getResourceAsStream("/" + fileName);
            try {
                if (in != null) {
                    System.out.println("Config.loadProperties():" + fileName);
                    config.load(in);
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Config getInstance() {
        return instance;
    }

    public String getProperty(String key) {
        return config.getProperty(key);
    }

}
