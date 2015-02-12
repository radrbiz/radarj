package org.radarlab.client;

import org.radarlab.core.exception.RadarException;

import java.util.Map;

public interface ClientProcessor {

    static final String URI = "http://"+ org.radarlab.client.util.Config.getInstance().getProperty("client.server.host")+":"+ org.radarlab.client.util.Config.getInstance().getProperty("client.server.port");
    static final String ADMIN_URI = "http://"+ org.radarlab.client.util.Config.getInstance().getProperty("client.server.admin.host") + ":"+ org.radarlab.client.util.Config.getInstance().getProperty("client.server.port");
    static final String MODEL_SERVER = org.radarlab.client.util.Config.getInstance().getProperty("model.api.server");

    public String processResponse(Map<String, String> params) throws RadarException;
}
