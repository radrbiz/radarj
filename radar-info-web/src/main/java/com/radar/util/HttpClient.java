package com.radar.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.radarlab.client.Config;
import org.radarlab.core.exception.RadarException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class HttpClient {

    private static final Logger logger = Logger.getLogger(HttpClient.class.getName());
    public static final String HTTP_SERVER_ADDRESS = Config.getInstance().getProperty("http.server.backend");

    public static Response post(String url, String data) throws RadarException {
        HttpURLConnection conn = null;
        try {
            URL requestURL = new URL(url);
            byte[] bytes = data.getBytes("utf-8");
            conn = (HttpURLConnection) requestURL.openConnection();
            conn.setRequestProperty("User-Agent", "radar-Agent");
            conn.setRequestProperty("Accept-Encoding", "gzip");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(20 * 1000);
            conn.setReadTimeout(20 * 1000);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.getOutputStream().write(bytes);
            String encode = conn.getContentEncoding();
            InputStream inStream;
            if (StringUtils.isNotBlank(encode) && encode.toLowerCase().contains("gzip")) {
                inStream = new GZIPInputStream(conn.getInputStream());
            } else {
                inStream = conn.getInputStream();
            }
            byte[] inputBytes = readInputStream(inStream);
            String response = new String(inputBytes, "utf-8");
            logger.debug("response from peer->" + response);
            Response resp = new Response();
            resp.setRequestLength(bytes.length);
            resp.setResponseLength(inputBytes.length);
            resp.setResponseString(response);
            return resp;
        }catch (Exception e){
            e.printStackTrace();
            logger.warn("error to sending http request.." + e.getMessage(),e);
            throw new RadarException("error when sending request, message:" + e.getMessage());
        }finally {
            if(conn != null){
                conn.disconnect();
            }
        }
    }

    public static Response get(String url) throws RadarException {
        HttpURLConnection conn = null;
        try {
            URL requestURL = new URL(url);
            conn = (HttpURLConnection) requestURL.openConnection();
            conn.setRequestProperty("User-Agent", "radar-Agent");
            conn.setRequestProperty("Accept-Encoding", "gzip");
            conn.setConnectTimeout(6 * 1000);
            conn.setReadTimeout(6 * 1000);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("GET");
            String encode = conn.getContentEncoding();
            InputStream inStream;
            if (StringUtils.isNotBlank(encode) && encode.toLowerCase().contains("gzip")) {
                inStream = new GZIPInputStream(conn.getInputStream());
            } else {
                inStream = conn.getInputStream();
            }
            byte[] inputBytes = readInputStream(inStream);
            String response = new String(inputBytes, "utf-8");
            logger.debug("response from peer->" + response);
            Response resp = new Response();
            resp.setResponseLength(inputBytes.length);
            resp.setResponseString(response);
            return resp;
        }catch (Exception e){
            e.printStackTrace();
            logger.warn("error to sending http request.." + e.getMessage(),e);
            throw new RadarException("error when sending request, message:" + e.getMessage());
        }finally {
            if(conn != null){
                conn.disconnect();
            }
        }
    }


    public static String post(String url, Map<String, String> map) throws IOException {
        logger.info("post to:"+url + ", params="+JSONObject.valueToString(map));
        HttpPost httppost = new HttpPost(url);
        Iterator iter = map.entrySet().iterator();
        List<NameValuePair> params=new ArrayList<NameValuePair>();
        while(iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key  = (String) entry.getKey();
            String value = (String) entry.getValue();
            params.add(new BasicNameValuePair(key, value));
        }

        httppost.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));

        DefaultHttpClient httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,5000);
        httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 5000);

        HttpResponse response=httpClient.execute(httppost);
        String result = null;
        if(response.getStatusLine().getStatusCode()==200) {
            result = EntityUtils.toString(response.getEntity());
            logger.info("post to:"+url + ", res="+result);
        }else {
            try {
                logger.error("code="+response.getStatusLine().getStatusCode()+",res="+EntityUtils.toString(response.getEntity()));
            }catch (Exception e){
                logger.error(e.getMessage(),e);
            }
        }

        return result;
    }

    private static byte[] readInputStream(InputStream inStream) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len ;
        while ((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        byte[] data = outStream.toByteArray();
        outStream.close();
        inStream.close();
        return data;
    }

    public static class Response{
        private int requestLength;
        private int responseLength;
        private String responseString;

        public int getRequestLength() {
            return requestLength;
        }

        public void setRequestLength(int requestLength) {
            this.requestLength = requestLength;
        }

        public int getResponseLength() {
            return responseLength;
        }

        public void setResponseLength(int responseLength) {
            this.responseLength = responseLength;
        }

        public String getResponseString() {
            return responseString;
        }

        public void setResponseString(String responseString) {
            this.responseString = responseString;
        }
    }
}
