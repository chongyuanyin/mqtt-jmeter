package net.xmeter.samplers.mqtt.paho;

import java.util.logging.Logger;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import net.xmeter.samplers.mqtt.MQTTSubListener;

public class PahoSubCallback implements MqttCallback {
	
	private static final Logger logger = Logger.getLogger(PahoSubCallback.class.getCanonicalName());
	
	private final MQTTSubListener subListener;
	
	PahoSubCallback(MQTTSubListener subListener) {
		this.subListener = subListener;
	}

	@Override
	public void disconnected(MqttDisconnectResponse disconnectResponse) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mqttErrorOccurred(MqttException exception) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		logger.fine("!!Msg arrived: id=" + message.getId() + ", payload=" + new String(message.getPayload()));
		subListener.accept(topic, new String(message.getPayload(), "UTF-8"), message.getProperties().getUserProperties());
	}

	@Override
	public void deliveryComplete(IMqttToken token) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void connectComplete(boolean reconnect, String serverURI) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void authPacketArrived(int reasonCode, MqttProperties properties) {
		// TODO Auto-generated method stub
		
	}

}
