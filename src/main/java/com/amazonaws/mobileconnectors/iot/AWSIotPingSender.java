package com.amazonaws.mobileconnectors.iot;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.paho.client.mqttv3.MqttToken;
import org.eclipse.paho.client.mqttv3.TimerPingSender;
import org.eclipse.paho.client.mqttv3.internal.ClientComms;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPingReq;

final class AWSIotPingSender extends TimerPingSender {
    private static final Log LOGGER = LogFactory.getLog(AWSIotPingSender.class);

    private ClientComms comms;
    private static final long PING_PERIOD = 10000L;

    private Timer heartbeatPingTimer;
    private final Timer disconnectingCheckTimer = new Timer("Disconnecting check timer "+System.currentTimeMillis());

	private final AWSIotMqttManager manager;
    
    public AWSIotPingSender(AWSIotMqttManager manager) {
    	this.manager = manager;
    	disconnectingCheckTimer.schedule(new DisconnectingCheckTask(), 1L, 20000L);
    }

    @Override
    public void init(ClientComms comms) {
        this.comms = comms;
        super.init(comms);
    }

    // on successful connect timers are started. KeepAlive timer will continue
    // heartbeat timer is also started.
    public void start() {
        super.start();
        heartbeatPingTimer = new Timer("Heartbeat Ping Timer "+System.currentTimeMillis());
        heartbeatPingTimer.schedule(new HeartbeatPingTask(), 1L, PING_PERIOD);
    }

    public void stop() {
        super.stop();
        if (heartbeatPingTimer != null) {
            heartbeatPingTimer.cancel();
            heartbeatPingTimer = null;
        }
    }

    private final class HeartbeatPingTask extends TimerTask {
        @Override
        public void run() {
            try {
                LOGGER.info("MQTT PING : Heartbeat ping request");
                comms.sendNoWait(new MqttPingReq(), new MqttToken(comms.getClient().getClientId()));
            } catch (Exception e) {
                LOGGER.info(e);
            }
        }
    }

    // Timer task to reinitialize mqttAsync client when it gets stuck in disconnecting state.
    private final class DisconnectingCheckTask extends TimerTask {
		@Override
		public void run() {
			try {
				if (comms.isDisconnecting()) {
					LOGGER.info("Detected disconnecting state. Triggering reInitialize.");
					manager.disconnectAndInitialize();
				}
			} catch(Exception e) {
				LOGGER.debug(e);
			}
		}
    }

    // Disconnectingtimer task scheduler is long-lived compared to other timers in this class.
    // However, when we reinitialize asyncClient we have to ensure that this scheduler and task are shutdown 
    // before reinitializing new asyncClient
	public void cancelAllTimers() {
        stop();
        disconnectingCheckTimer.cancel();
	}
}
