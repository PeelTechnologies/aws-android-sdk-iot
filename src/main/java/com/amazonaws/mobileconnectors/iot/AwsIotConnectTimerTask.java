package com.amazonaws.mobileconnectors.iot;

import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.paho.client.mqttv3.MqttException;

class AwsIotConnectTimerTask extends TimerTask {
    private static final Log LOGGER = LogFactory.getLog(AwsIotConnectTimerTask.class);

    private final AWSIotMqttManager iotMqttManager;
    private boolean autoReconnectActive;

    public AwsIotConnectTimerTask(AWSIotMqttManager iotMqttManager) {
        this.iotMqttManager = iotMqttManager;
    }

    void setAutoReconnectActive(boolean autoReconnectActive) {
        this.autoReconnectActive = autoReconnectActive;
    }

    public void run() {
        LOGGER.debug("MQTT ping : autoReconnect is " + autoReconnectActive);
        // true only if previous connect attempt was successful
        if (autoReconnectActive) {
            // connect initially succeeded standard reconnect logic in paho will work, do nothing
            try {
                iotMqttManager.reconnect();
            } catch (MqttException e) {
                LOGGER.error("Exception reason code: " + e.getReasonCode(), e);
                if (e.getReasonCode() == MqttException.REASON_CODE_CLIENT_DISCONNECTING) {
                    iotMqttManager.disconnectAndInitialize();
                }
            }
        } else {
            // connect initially failed we need to trigger auto-reconnect and let it handle with correctly starting the cycle
            try {
                iotMqttManager.disconnectAndInitialize();
            } catch (Exception e) {
                LOGGER.error(e);
            }
        }
    }
}
