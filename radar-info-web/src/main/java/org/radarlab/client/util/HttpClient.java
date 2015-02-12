package org.radarlab.client.util;

import org.radarlab.core.exception.RadarException;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

public class HttpClient {

    private static final Logger logger = Logger.getLogger(HttpClient.class.getName());

    public static Response post(String url, String data) throws RadarException {
        HttpURLConnection conn = null;
        try {
            URL requestURL = new URL(url);
            byte[] bytes = data.getBytes("utf-8");
            conn = (HttpURLConnection) requestURL.openConnection();
            conn.setRequestProperty("User-Agent", "99coin-Agent");
            conn.setRequestProperty("Accept-Encoding", "gzip");
            conn.setConnectTimeout(6 * 1000);
            conn.setReadTimeout(6 * 1000);
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
            logger.warn("error to sending http request.." + e.getMessage());
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
            conn.setRequestProperty("User-Agent", "99coin-Agent");
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
            logger.warn("error to sending http request.." + e.getMessage());
            throw new RadarException("error when sending request, message:" + e.getMessage());
        }finally {
            if(conn != null){
                conn.disconnect();
            }
        }
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
