package net.xmeter.samplers;

import net.xmeter.Constants;

public class TimestampUtil {
	
	public static byte[] appendTimestamp(byte[] payload) {
		byte[] toSend = new byte[]{};
		byte[] timePrefix = (System.currentTimeMillis() + Constants.TIME_STAMP_SEP_FLAG).getBytes();
		toSend = new byte[timePrefix.length + payload.length];
		System.arraycopy(timePrefix, 0, toSend, 0, timePrefix.length);
		System.arraycopy(payload, 0, toSend, timePrefix.length , payload.length);
		return toSend;
	}

	public static long getTimestamp(String msg) {
		int index = msg.indexOf(Constants.TIME_STAMP_SEP_FLAG);
		if (index == -1) {
			return -1;
		} else {
			long timestamp = Long.parseLong(msg.substring(0, index));
			return timestamp;
		}
	}
}
