package net.xmeter.samplers.mqtt.paho;

import net.xmeter.samplers.mqtt.MQTTFactory;
import net.xmeter.samplers.mqtt.MQTTSpi;

public class PahoMQTTSpi implements MQTTSpi {

	@Override
	public MQTTFactory factory() {
		return new PahoMQTTFactory();
	}

}
