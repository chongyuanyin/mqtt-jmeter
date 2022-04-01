package net.xmeter.samplers.mqtt.fuse;

import java.net.URISyntaxException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Tracer;
import org.fusesource.mqtt.codec.MQTTFrame;
import org.fusesource.mqtt.codec.PUBCOMP;
import org.fusesource.mqtt.codec.PUBREC;

import net.xmeter.samplers.mqtt.ConnectionParameters;
import net.xmeter.samplers.mqtt.MQTTClient;
import net.xmeter.samplers.mqtt.MQTTClientException;
import net.xmeter.samplers.mqtt.MQTTConnection;

class FuseMQTTClient implements MQTTClient {
    private static final Logger logger = Logger.getLogger(FuseMQTTClient.class.getCanonicalName());

    private final ConnectionParameters parameters;
    private final MQTT mqtt = new MQTT();
    
    private static ConcurrentHashMap<Short, Long> elapsedMap = new ConcurrentHashMap<>();
    private static float avg = 0;
    private static long count = 0;

    FuseMQTTClient(ConnectionParameters parameters) throws URISyntaxException {
        if (!FuseUtil.ALLOWED_PROTOCOLS.contains(parameters.getProtocol())) {
            throw new IllegalArgumentException("Unsupported protocol" + parameters.getProtocol());
        }
        this.parameters = parameters;

        mqtt.setClientId(parameters.getClientId());
        mqtt.setHost(createHostAddress(parameters));
        mqtt.setCleanSession(parameters.isCleanSession());
        mqtt.setKeepAlive(parameters.getKeepAlive());
        mqtt.setUserName(parameters.getUsername());
        mqtt.setPassword(parameters.getPassword());
        if (parameters.isSecureProtocol()) {
            mqtt.setSslContext(((FuseMQTTSsl) parameters.getSsl()).getSslContext());
        }
        mqtt.setVersion(parameters.getVersion());
        mqtt.setConnectAttemptsMax(parameters.getConnectMaxAttempts());
        mqtt.setReconnectAttemptsMax(parameters.getReconnectMaxAttempts());
        mqtt.setReceiveBufferSize(1024*4);
        mqtt.setSendBufferSize(1024*4);

        mqtt.setTracer(new Tracer() {
            @Override
            public void onSend(MQTTFrame frame) {
            		try {
            			if (frame.messageType() == PUBREC.TYPE ) {
                			PUBREC ack = new PUBREC().decode(frame);
                			elapsedMap.put(ack.messageId(), System.currentTimeMillis());
                		} else if (frame.messageType() == PUBCOMP.TYPE) {
                			PUBCOMP ack = new PUBCOMP().decode(frame);
                			if (elapsedMap.get(ack.messageId()) != null) {
                				long elapsed = System.currentTimeMillis() - elapsedMap.get(ack.messageId());
                				count++;
                				avg = avg + ((float)elapsed - avg)/(float)count;
                				if (count % 100 == 0) {
                					logger.fine(() -> "avg elapsed=" + avg);
                				}
                				elapsedMap.remove(ack.messageId());
                			}
                		}
            		} catch (Exception e) {
            			
            		}
            		
            }
        });
    }

    private String createHostAddress(ConnectionParameters parameters) {
        return parameters.getProtocol().toLowerCase() + "://" + parameters.getHost() + ":" + parameters.getPort();
    }

    @Override
    public String getClientId() {
        return mqtt.getClientId().toString();
    }

    @Override
    public MQTTConnection connect() throws Exception {
        Semaphore connLock = new Semaphore(0);
        CallbackConnection connection = mqtt.callbackConnection();
        ConnectionCallback callback = new ConnectionCallback(mqtt, parameters, connLock);
        connection.connect(callback);
        try {
            connLock.tryAcquire(parameters.getConnectTimeout(), TimeUnit.SECONDS);
            return new FuseMQTTConnection(mqtt.getClientId().toString(), callback, connection);
        } catch (InterruptedException e) {
            Semaphore killLock = new Semaphore(0);
            connection.kill(new Callback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    killLock.release();
                }

                @Override
                public void onFailure(Throwable throwable) {
                    logger.log(Level.SEVERE, "Failed to kill" + mqtt, throwable);
                    killLock.release();
                }
            });
            killLock.acquire();
            throw new MQTTClientException("Connection timeout " + mqtt, e);
        }
    }
}
