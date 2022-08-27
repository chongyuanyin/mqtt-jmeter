package net.xmeter.samplers.mqtt.paho;

import org.eclipse.paho.mqttv5.client.IMqttMessageListener;
import org.eclipse.paho.mqttv5.common.MqttMessage;

public class PahoMQTTMsgListener implements IMqttMessageListener {
	
//	private final Runnable onSuccess;
//	private final Consumer<Throwable> onFailure;
//	
//	PahoMQTTMsgListener(Runnable onSuccess, Consumer<Throwable> onFailure) {
//		this.onSuccess = onSuccess;
//		this.onFailure = onFailure;
//	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		// TODO Auto-generated method stub
		
	}

}
