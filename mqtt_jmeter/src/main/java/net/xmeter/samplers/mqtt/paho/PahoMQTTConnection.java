package net.xmeter.samplers.mqtt.paho;

import java.util.function.Consumer;
import java.util.logging.Logger;

import org.eclipse.paho.mqttv5.client.IMqttMessageListener;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.common.MqttException;

import net.xmeter.samplers.mqtt.MQTTConnection;
import net.xmeter.samplers.mqtt.MQTTPubResult;
import net.xmeter.samplers.mqtt.MQTTQoS;
import net.xmeter.samplers.mqtt.MQTTSubListener;

public class PahoMQTTConnection implements MQTTConnection {
	
	private static final Logger logger = Logger.getLogger(PahoMQTTConnection.class.getCanonicalName());
	
	private final String clientId;
	private final MqttClient client;
	private final MqttCallback connectionCallback;
	private MQTTSubListener listener;
	
	PahoMQTTConnection(String clientId, MqttClient client, MqttCallback connectionCallback) {
		this.clientId = clientId;
		this.client = client;
		this.connectionCallback = connectionCallback;
	}

	@Override
	public boolean isConnectionSucc() {
		return client.isConnected();
	}

	@Override
	public String getClientId() {
		return this.clientId;
	}

	@Override
	public void disconnect() throws Exception {
		client.disconnect();
	}

	@Override
	public MQTTPubResult publish(String topicName, byte[] message, MQTTQoS mqttQos, boolean retained) {
		try {
			client.publish(topicName, message, mqttQos.toValue(), retained);
			return new MQTTPubResult(true);
		} catch (MqttException ex) {
			return new MQTTPubResult(false, ex.getMessage());
		}
	}

	@Override
	public void subscribe(String[] topicNames, MQTTQoS mqttQos, Runnable onSuccess, Consumer<Throwable> onFailure) {
		int[] qos = new int[topicNames.length];
		for (int i=0; i<topicNames.length; i++) {
			qos[i] = mqttQos.toValue();
		}
		
		try {
			client.setCallback(new PahoSubCallback());
			client.subscribe(topicNames, qos, null);
			onSuccess.run();
			client.setCallback(null);
		} catch (MqttException ex) {
			onFailure.accept(ex);
		}
		
	}

	@Override
	public void setSubListener(MQTTSubListener listener) {
		this.listener = listener;
	}

	
}
