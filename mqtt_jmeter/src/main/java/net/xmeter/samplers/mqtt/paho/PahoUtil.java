package net.xmeter.samplers.mqtt.paho;

import java.util.ArrayList;
import java.util.List;

import net.xmeter.Constants;

public class PahoUtil {
	static final List<String> ALLOWED_PROTOCOLS;
    static {
        ALLOWED_PROTOCOLS = new ArrayList<>();
        ALLOWED_PROTOCOLS.add(Constants.TCP_PROTOCOL);
//        ALLOWED_PROTOCOLS.add(Constants.SSL_PROTOCOL);
    }

}
