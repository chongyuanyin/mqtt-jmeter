package net.xmeter.samplers.mqtt.paho;

import java.util.logging.Logger;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import net.xmeter.Constants;
import net.xmeter.samplers.mqtt.ConnectionParameters;
import net.xmeter.samplers.mqtt.MQTT;
import net.xmeter.samplers.mqtt.MQTTClient;
import net.xmeter.samplers.mqtt.MQTTConnection;
import net.xmeter.samplers.mqtt.MQTTUtil;

public class PahoMQTTClient implements MQTTClient {
	
	private static final Logger logger = Logger.getLogger(PahoMQTTClient.class.getCanonicalName());

    private final ConnectionParameters parameters;
    private final MqttClient mqttClient;
	
	PahoMQTTClient(ConnectionParameters parameters) throws MqttException {
		if (!MQTT.getSupportedProtocols(Constants.PAHO_MQTT_CLIENT_NAME).contains(parameters.getProtocol())) {
			throw new IllegalArgumentException("Unsupported protocol" + parameters.getProtocol());
		}
		
		this.parameters = parameters;
		
		mqttClient = new MqttClient(MQTTUtil.createHostAddress(parameters), parameters.getClientId(), new MemoryPersistence());
	}

	@Override
	public String getClientId() {
		return mqttClient.getClientId();
	}

	@Override
	public MQTTConnection connect() throws Exception {
		MqttConnectionOptions connOpts = new MqttConnectionOptions();
		connOpts.setKeepAliveInterval(parameters.getKeepAlive());
		connOpts.setCleanStart(parameters.isCleanSession());
		connOpts.setUserName(parameters.getUsername());
		if (parameters.getPassword() != null) {
			connOpts.setPassword(parameters.getPassword().getBytes("UTF-8"));
		}
		connOpts.setConnectionTimeout(parameters.getConnectTimeout());
		//TODO reconnect
		
		MqttCallback connCallbak = new MqttCallback() {

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
				// TODO Auto-generated method stub
			}

			@Override
			public void deliveryComplete(IMqttToken token) {
				// TODO Auto-generated method stub
			}

			@Override
			public void connectComplete(boolean reconnect, String serverURI) {
//				logger.info("connectComplete " + getClientId() + System.currentTimeMillis());
			}

			@Override
			public void authPacketArrived(int reasonCode, MqttProperties properties) {
				// TODO Auto-generated method stub
			}
		};
		mqttClient.setCallback(connCallbak);
		
//		logger.info(getClientId() + ": Connecting to " + mqttClient.getServerURI() + " " + System.currentTimeMillis());
		
		mqttClient.connect(connOpts);
		
//		logger.info(getClientId() + ": Connected " + System.currentTimeMillis());
		
		// TODO Auto-generated method stub
		return new PahoMQTTConnection(mqttClient.getClientId(), mqttClient, connCallbak);
	}

}
