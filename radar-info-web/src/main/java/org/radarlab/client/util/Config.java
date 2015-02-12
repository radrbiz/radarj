package org.radarlab.client.util;

import java.io.*;
import java.util.Properties;

public class Config {
    Properties config = new Properties();
    private static Config instance = new Config();

    private Config(){
        synchronized (Config.class) {
            loadProperties();
        }
    }

    private void loadProperties(){
        String[] fileNames = new String[]{"config.properties", "websocket.properties"};
        for(String fileName: fileNames) {
            File confFile = new File("/etc/" + fileName);
            InputStream in;
            if (confFile.exists()) {
                try {
                    in = new FileInputStream(confFile);
                } catch (FileNotFoundException e) {
                    in = getClass().getResourceAsStream("/" + fileName);
                }
            } else {
                in = getClass().getResourceAsStream("/" + fileName);
            }
            try {
                if(in != null)
                    config.load(in);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Config getInstance() {
        return instance;
    }

    public String getProperty(String key){
        return config.getProperty(key);
    }

    public static void main(String[] args){
        String name = Config.getInstance().getProperty("client.name");
        System.out.println(name);
    }

}
