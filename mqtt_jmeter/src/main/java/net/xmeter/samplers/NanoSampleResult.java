package net.xmeter.samplers;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NanoSampleResult extends SampleResult {
	
	private static final long serialVersionUID = -862214556221758172L;

	// Needs to be accessible from Test code
    static Logger log = LoggerFactory.getLogger(SampleResult.class);
    
    enum SampleTimeUnit {
    	MS, US, NS
    }
	
    /**
     * timeStamp == 0 means either not yet initialised or no stamp available (e.g. when loading a results file)
     * the time stamp - can be start or end
     */
    private long timeStamp = 0;

    private long startTime = 0;
    
    private long endTime = 0;
    
    private long startTimeInMs = 0;
    
    private long endTimeInMs = 0;

    private long idleTime = 0;// Allow for non-sample time
    
    /** elapsed time */
    private long elapsedTime = 0;
    
    /** time to first response */
    private long latency = 0;
    
    /** time to end connecting */
    private long connectTime = 0;

    /** Start of pause (if any) */
    private long pauseTime = 0;
    
    private final BigDecimal nanoTimeOffset;
    
    // Allow testcode access to the settings
    final SampleTimeUnit timeUnit;
    
    final long nanoThreadSleep;
    
    private static final String INVALID_CALL_SEQUENCE_MSG = "Invalid call sequence"; // $NON-NLS-1$
    
    private static final boolean START_TIMESTAMP = false;
    
    private static final SampleTimeUnit USE_NANO_TIME = SampleTimeUnit.US;  // $NON-NLS-1$
    
    private static final long NANOTHREAD_SLEEP = 0;
    
    public NanoSampleResult() {
        this(USE_NANO_TIME, NANOTHREAD_SLEEP);
    }
    
    NanoSampleResult(SampleTimeUnit timeUnit, long nanoThreadSleep) {
        this.elapsedTime = 0;
        this.timeUnit = timeUnit;
        this.nanoThreadSleep = nanoThreadSleep;
        this.nanoTimeOffset = initOffset();
    }
    
    private BigDecimal initOffset(){
    	switch (this.timeUnit) {
    	case US:
    	case NS:
    		return nanoThreadSleep > 0 ? NanoOffset.getNanoOffset() : new BigDecimal(System.currentTimeMillis()).multiply(new BigDecimal(1000000)).subtract(new BigDecimal(sampleNsClock())) ;
    	default:
    		return new BigDecimal(Long.MIN_VALUE);
    	}
    }
    
    private static long sampleNsClock() {
        return System.nanoTime();
    }
    
    public long currentTime() {
    	if (timeUnit == SampleTimeUnit.MS){
    		return System.currentTimeMillis();
    	} else {
    		if (nanoTimeOffset.compareTo(new BigDecimal(Long.MIN_VALUE)) == 0) {
                throw new IllegalStateException("Invalid call; nanoTimeOffset has not been set");
            }
    		BigDecimal nanoTime = new BigDecimal(sampleNsClock()).add(nanoTimeOffset);
    		if (timeUnit == SampleTimeUnit.NS) {
    			return nanoTime.longValue();
    		} else {
    			return nanoTime.divide(new BigDecimal(1000)).longValue();
    		}
    	}
    }
	
	/**
     * Record the start time of a sample
     *
     */
    public void sampleStart() {
        if (startTime == 0) {
        	startTime = currentTime();
        	startTimeInMs = System.currentTimeMillis();
        	if (START_TIMESTAMP) {
                timeStamp = startTime;
            }
        } else {
            log.error("sampleStart called twice", new Throwable(INVALID_CALL_SEQUENCE_MSG));
        }
    }
    
    /**
     * Record the end time of a sample and calculate the elapsed time
     *
     */
    public void sampleEnd() {
        if (endTime == 0) {
            setEndTime(currentTime());
//            endTimeInMs = System.currentTimeMillis();
//            endTime = endTimeInMs;
//            startTime = startTimeInMs;
            super.setStartTime(startTime);
            super.setEndTime(endTime);
        } else {
            log.error("sampleEnd called twice", new Throwable(INVALID_CALL_SEQUENCE_MSG));
        }
    }
    
    public void setEndTime(long end) {
        endTime = end;
        if (!START_TIMESTAMP) {
            timeStamp = endTime;
        }
        if (startTime == 0) {
            log.error("setEndTime must be called after setStartTime", new Throwable(INVALID_CALL_SEQUENCE_MSG));
        } else {
            elapsedTime = endTime - startTime - idleTime;
        }
    }
    
    /**
     * Pause a sample
     *
     */
    public void samplePause() {
        if (pauseTime != 0) {
            log.error("samplePause called twice", new Throwable(INVALID_CALL_SEQUENCE_MSG));
        }
        pauseTime = currentTime();
    }
    
    /**
     * Resume a sample
     *
     */
    public void sampleResume() {
        if (pauseTime == 0) {
            log.error("sampleResume without samplePause", new Throwable(INVALID_CALL_SEQUENCE_MSG));
        }
        idleTime += currentTime() - pauseTime;
        pauseTime = 0;
    }
	
    /**
     * Set the time to the first response
     *
     */
    public void latencyEnd() {
        latency = currentTime() - startTime - idleTime;
    }
    
    /**
     * Set the time to the end of connecting
     */
    public void connectEnd() {
        connectTime = currentTime() - startTime - idleTime;
    }
    
    public void setStampAndTime(long stamp, long elapsed) {
        if (startTime != 0 || endTime != 0){
            throw new IllegalStateException("Calling setStampAndTime() after start/end times have been set");
        }
        stampAndTime(stamp, elapsed);
    }
    
    // Helper method to maintain timestamp relationships
    private void stampAndTime(long stamp, long elapsed) {
        if (START_TIMESTAMP) {
            startTime = stamp;
            endTime = stamp + elapsed;
        } else {
            startTime = stamp - elapsed;
            endTime = stamp;
        }
        timeStamp = stamp;
        elapsedTime = elapsed;
    }
    
    /**
     * @return the start time
     */
    public long getStartTime() {
        return startTime;
    }
    
    /**
     * Get the sample timestamp, which may be either the start time or the end time.
     *
     * @see #getStartTime()
     * @see #getEndTime()
     *
     * @return timeStamp in milliseconds
     */
    public long getTimeStamp() {
        return timeStamp;
    }
    
    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }
    
    /**
     * Get the time it took this sample to occur.
     *
     * @return elapsed time in milliseconds
     *
     */
    public long getTime() {
        return elapsedTime;
    }
    
    /**
     * @return idleTime
     */
    public long getIdleTime() {
        return idleTime;
    }
    
    public void setIdleTime(long idle) {
        idleTime = idle;
    }
    
    /**
     * @return Returns the latency.
     */
    public long getLatency() {
        return latency;
    }
    
    public void setLatency(long latency) {
        this.latency = latency;
    }
    
    /**
     * @return Returns the connect time.
     */
    public long getConnectTime() {
        return connectTime;
    }
    
    public void setConnectTime(long time) {
        this.connectTime = time;
    }
    
    private static class NanoOffset extends Thread {

        private static volatile BigDecimal nanoOffset;

        static BigDecimal getNanoOffset() {
            return nanoOffset;
        }

        @Override
        public void run() {
            // Wait longer than a clock pulse (generally 10-15ms)
            getOffset(30L); // Catch an early clock pulse to reduce slop.
            while(true) {
                getOffset(NANOTHREAD_SLEEP); // Can now afford to wait a bit longer between checks
            }
        }

        private static void getOffset(long wait) {
            try {
                TimeUnit.MILLISECONDS.sleep(wait);
                long clockInMs = System.currentTimeMillis();
                long nano = NanoSampleResult.sampleNsClock();
                nanoOffset = new BigDecimal(clockInMs).multiply(new BigDecimal(1000000)).subtract(new BigDecimal(nano));
            } catch (InterruptedException ignore) {
                // ignored
                Thread.currentThread().interrupt();
            }
        }
    }
}
