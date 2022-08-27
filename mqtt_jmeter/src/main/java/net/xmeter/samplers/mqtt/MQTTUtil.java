package net.xmeter.samplers.mqtt;

public class MQTTUtil {
	
	public static String createHostAddress(ConnectionParameters parameters) {
        return parameters.getProtocol().toLowerCase() + "://" + parameters.getHost() + ":" + parameters.getPort();
    }

}
