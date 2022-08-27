package net.xmeter.samplers.mqtt.paho;

import java.util.List;

import net.xmeter.Constants;
import net.xmeter.samplers.AbstractMQTTSampler;
import net.xmeter.samplers.mqtt.ConnectionParameters;
import net.xmeter.samplers.mqtt.MQTTClient;
import net.xmeter.samplers.mqtt.MQTTFactory;
import net.xmeter.samplers.mqtt.MQTTSsl;

public class PahoMQTTFactory implements MQTTFactory {

	@Override
	public String getName() {
		return Constants.PAHO_MQTT_CLIENT_NAME;
	}

	@Override
	public List<String> getSupportedProtocols() {
		return PahoUtil.ALLOWED_PROTOCOLS;
	}

	@Override
	public MQTTClient createClient(ConnectionParameters parameters) throws Exception {
		return new PahoMQTTClient(parameters);
	}

	@Override
	public MQTTSsl createSsl(AbstractMQTTSampler sampler) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
