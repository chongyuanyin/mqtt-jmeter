package net.xmeter.samplers.mqtt;

import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.paho.mqttv5.common.packet.UserProperty;

import net.xmeter.SubBean;
import net.xmeter.samplers.SubSampler;
import net.xmeter.samplers.TimestampUtil;

public class MQTTSubListener {
	
	private static final Logger logger = Logger.getLogger(MQTTSubListener.class.getCanonicalName());
	
	private final boolean sampleByTime;
	private final int sampleCount;
	private SubSampler subSampler;
	
	private boolean printFlag = false;
	
	
	public MQTTSubListener(boolean sampleByTime, int sampleCount, SubSampler subSampler) {
		this.sampleByTime = sampleByTime;
		this.sampleCount = sampleCount;
		this.subSampler = subSampler;
	}
	
    public void accept(String topic, String message, Runnable ack) {
    	if (ack != null) {
    		ack.run();
    	}

		if(sampleByTime) {
			synchronized (subSampler.getDataLock()) {
				handleSubBean(sampleByTime, message, sampleCount);
			}
		} else {
			synchronized (subSampler.getDataLock()) {
				SubBean bean = handleSubBean(sampleByTime, message, sampleCount);
				if(bean.getReceivedCount() == sampleCount) {
					subSampler.getDataLock().notify();
				}
			}
		}
    }
    
    public void accept(String topic, String message, List<UserProperty> userProps) {
    	if(sampleByTime) {
			synchronized (subSampler.getDataLock()) {
				handleSubBean(sampleByTime, message, sampleCount, userProps);
			}
		} else {
			synchronized (subSampler.getDataLock()) {
				SubBean bean = handleSubBean(sampleByTime, message, sampleCount, userProps);
				if(bean.getReceivedCount() == sampleCount) {
					subSampler.getDataLock().notify();
				}
			}
		}
    }
    
    private SubBean handleSubBean(boolean sampleByTime, String msg, int sampleCount) {
    	return handleSubBean(sampleByTime, msg, sampleCount, null);
    }
    
    private SubBean handleSubBean(boolean sampleByTime, String msg, int sampleCount, List<UserProperty> userProps) {
		SubBean bean = null;
		if(subSampler.getBatches().isEmpty()) {
			bean = new SubBean();
			subSampler.getBatches().add(bean);
		} else {
			SubBean[] beans = new SubBean[subSampler.getBatches().size()];
			subSampler.getBatches().toArray(beans);
			bean = beans[beans.length - 1];
		}
		
		if((!sampleByTime) && (bean.getReceivedCount() == sampleCount)) { //Create a new batch when latest bean is full.
			logger.info("The tail bean is full, will create a new bean for it.");
			bean = new SubBean();
			subSampler.getBatches().add(bean);
		}
		boolean handled = false;
		if (userProps != null) {
			String msgInNanoTime = null;
			String msgOutNanoTime = null;
			for(UserProperty prop: userProps) {
				if (prop.getKey().equals("message_in")) {
					msgInNanoTime = prop.getValue();
				} else if (prop.getKey().equals("message_out")) {
					msgOutNanoTime = prop.getValue();
				}
				if (msgInNanoTime != null && msgOutNanoTime != null) {
					break;
				}
			}
			if (msgInNanoTime != null && msgOutNanoTime != null) {
				logger.info(MessageFormat.format("msgIn: {0}, msgOut: {1}", msgInNanoTime, msgOutNanoTime));
				long elapsed = 0;
				long elapsedInNano = Long.parseLong(msgOutNanoTime) - Long.parseLong(msgInNanoTime);
				if (subSampler.getTimeGranularity().equalsIgnoreCase("ms")) {
					elapsed = elapsedInNano / 1000000;
				} else if (subSampler.getTimeGranularity().equalsIgnoreCase("us")) {
					elapsed = elapsedInNano / 1000;
				}
				double avgElapsedTime = bean.getAvgElapsedTime();
				int receivedCount = bean.getReceivedCount();
				avgElapsedTime = (avgElapsedTime * receivedCount + elapsed) / (receivedCount + 1);
				bean.setAvgElapsedTime(avgElapsedTime);
				handled = true;
			}
		}
		if (!handled) {
			if (subSampler.isAddTimestamp()) {
				long now = System.currentTimeMillis();
				long start = TimestampUtil.getTimestamp(msg);
				if (start < 0 && !printFlag) {
					logger.info(() -> "Payload does not include timestamp: " + msg);
					printFlag = true;
				} else {
					long elapsed = now - start;
					
					double avgElapsedTime = bean.getAvgElapsedTime();
					int receivedCount = bean.getReceivedCount();
					avgElapsedTime = (avgElapsedTime * receivedCount + elapsed) / (receivedCount + 1);
					bean.setAvgElapsedTime(avgElapsedTime);
				}
			}
		}
		if (subSampler.isDebugResponse()) {
			bean.getContents().add(msg);
		}
		bean.setReceivedMessageSize(bean.getReceivedMessageSize() + msg.length());
		bean.setReceivedCount(bean.getReceivedCount() + 1);
		return bean;
	}
}
