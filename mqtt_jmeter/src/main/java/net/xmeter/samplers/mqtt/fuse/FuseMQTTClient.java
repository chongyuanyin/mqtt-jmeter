package net.xmeter.samplers.mqtt.fuse;

import java.net.URISyntaxException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Tracer;

import net.xmeter.samplers.mqtt.ConnectionParameters;
import net.xmeter.samplers.mqtt.MQTTClient;
import net.xmeter.samplers.mqtt.MQTTClientException;
import net.xmeter.samplers.mqtt.MQTTConnection;
import net.xmeter.samplers.mqtt.MQTTUtil;

class FuseMQTTClient implements MQTTClient {
    private static final Logger logger = Logger.getLogger(FuseMQTTClient.class.getCanonicalName());

    private final ConnectionParameters parameters;
    private final MQTT mqtt = new MQTT();

    FuseMQTTClient(ConnectionParameters parameters) throws URISyntaxException {
        if (!FuseUtil.ALLOWED_PROTOCOLS.contains(parameters.getProtocol())) {
            throw new IllegalArgumentException("Unsupported protocol" + parameters.getProtocol());
        }
        this.parameters = parameters;

        mqtt.setClientId(parameters.getClientId());
        mqtt.setHost(MQTTUtil.createHostAddress(parameters));
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
        mqtt.setSendBufferSize(1024 * parameters.getSendBuff());
        mqtt.setReceiveBufferSize(1024 * parameters.getRcvBuff());

        mqtt.setTracer(new Tracer() {
            @Override
            public void debug(String message, Object...args) {
                logger.info(() -> "MQTT Tracer - " + mqtt + "[" + parameters.getUsername() + "]: " + String.format(message, args));
            }
        });
        
        mqtt.getTracer().debug("send buffer size: " + mqtt.getSendBufferSize()/1024);
        mqtt.getTracer().debug("receive buffer size: " + mqtt.getReceiveBufferSize()/1024);
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
