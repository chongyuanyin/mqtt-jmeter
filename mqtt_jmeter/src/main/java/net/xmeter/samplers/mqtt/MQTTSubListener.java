package net.xmeter.samplers.mqtt;

import java.util.logging.Logger;

import net.xmeter.SubBean;
import net.xmeter.samplers.SubSampler;
import net.xmeter.samplers.TimestampUtil;

public class MQTTSubListener {
	
	private static final Logger logger = Logger.getLogger(MQTTSubListener.class.getCanonicalName());
	
	private final boolean sampleByTime;
	private final int sampleCount;
	private SubSampler subSampler;
	
//	private transient ConcurrentLinkedQueue<SubBean> batches = new ConcurrentLinkedQueue<>();
	private boolean printFlag = false;
	
	
	public MQTTSubListener(boolean sampleByTime, int sampleCount, SubSampler subSampler) {
		this.sampleByTime = sampleByTime;
		this.sampleCount = sampleCount;
	}
	
    public void accept(String topic, String message, Runnable ack) {
    	ack.run();

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
    
    private SubBean handleSubBean(boolean sampleByTime, String msg, int sampleCount) {
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
		if (subSampler.isDebugResponse()) {
			bean.getContents().add(msg);
		}
		bean.setReceivedMessageSize(bean.getReceivedMessageSize() + msg.length());
		bean.setReceivedCount(bean.getReceivedCount() + 1);
		return bean;
	}
}
