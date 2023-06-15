/*
 * Copyright (c) 2022.
 *
 * This file is part of DiscoAPI.
 *
 *     DiscoAPI is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     DiscoAPI is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with DiscoAPI.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.foojay.api.mqtt;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttClientState;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.suback.Mqtt3SubAck;
import io.foojay.api.util.Config;
import io.foojay.api.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.hivemq.client.mqtt.MqttGlobalPublishFilter.ALL;
import static java.nio.charset.StandardCharsets.UTF_8;


public class MqttManager3 {
    private static final Logger                LOGGER                 = LoggerFactory.getLogger(MqttManager3.class);
    private static final String                PRODUCTION_ENVIRONMENT = "production";
    private static final String                STAGING_ENVIRONMENT    = "staging";
    private static final String                TESTING_ENVIRONMENT    = "testing";
    private static final boolean               GHOST                  = !Config.INSTANCE.getFoojayApiEnvironment().equals(PRODUCTION_ENVIRONMENT) && !Config.INSTANCE.getFoojayApiEnvironment().equals(STAGING_ENVIRONMENT) && !Config.INSTANCE.getFoojayApiEnvironment().equals(TESTING_ENVIRONMENT);
    private              Mqtt3AsyncClient      asyncClient;
    private              AtomicBoolean         connected;
    private              List<MqttEvtObserver> observers;


    // ******************** Constructors **************************************
    public MqttManager3() {
        this.connected = new AtomicBoolean(Boolean.FALSE);
        this.observers = new CopyOnWriteArrayList<>();
        try {
            asyncClient = createAsyncClient();
            init();
        } catch (NullPointerException e) {
            LOGGER.error("Error connecting to MQTT broker {} on port {}. {}", Config.INSTANCE.getFoojayMqttBroker(), Config.INSTANCE.getFoojayMqttPort(), e.getMessage());
        }
    }


    private void init() {
        connect(true);
    }


    // ******************** Methods *******************************************
    public boolean isConnected() { return connected.get(); }

    /**
     * @param topic The Topic the message should be published to
     * @param msg   String that contains the message content
     * @return CompletableFuture containing the result of the publish
     */
    public CompletableFuture<Mqtt3Publish> publish(final String topic, final String msg) {
        return publish(topic, MqttQos.EXACTLY_ONCE, false, msg);
    }
    /**
     * @param topic  The Topic the message should be published to
     * @param qos    QOS 0 == AT_MOST_ONCE, QOS 1 == AT_LEAST_ONCE, QOS 2 == EXACTLY_ONCE
     * @param retain Message will be stored on broker
     * @param msg    String that contains the message content
     * @return CompletableFuture containing the result of the publish
     */
    public CompletableFuture<Mqtt3Publish> publish(final String topic, final MqttQos qos, final boolean retain, final String msg) {
        if (GHOST) { return new CompletableFuture<>(); }
        if (null == asyncClient || !asyncClient.getState().isConnected()) { connect(true); }
        return asyncClient.publishWith()
                          .topic(topic)
                          .qos(qos)
                          .retain(retain)
                          .payload(UTF_8.encode(msg))
                          .send()
                          .whenComplete((publish, throwable) -> {
                              if (throwable != null) {
                                  // Handle failure to publish
                                  LOGGER.error("Error sending mqtt msg to {} with content {}, error {}", topic, msg, throwable.getMessage());
                                  asyncClient.disconnect();
                              } else {
                                  // Handle successful publish, e.g. logging or incrementing a metric
                              }
                          });
    }

    public CompletableFuture<Mqtt3SubAck> subscribe(final String topic, final MqttQos qos) {
        if (GHOST) { return null; }
        if (null == asyncClient || !asyncClient.getState().isConnected()) { connect(true); }
        return asyncClient.subscribeWith()
                          .topicFilter(topic)
                          .qos(qos)
                          //.callback(publish -> {
                          //    if (publish.getPayload().isPresent()) {
                          //        fireMqttEvent(new MqttEvt(publish.getTopic().toString(), new String(publish.getPayloadAsBytes())));
                          //    }
                          //})
                          .send();
    }

    public void unsubscribe(final String topic) {
        if (GHOST) { return; }
        if (null == asyncClient || !asyncClient.getState().isConnected()) { connect(true); }
        asyncClient.unsubscribeWith().topicFilter(topic).send();
    }

    public void connect(final boolean cleanStart) {
        if (GHOST) { return; }
        if (null == asyncClient) {
            try {
                asyncClient = createAsyncClient();
            } catch (Exception e) {
                LOGGER.error("Error connecting to MQTT broker {} on port {}. {}", Config.INSTANCE.getFoojayMqttBroker(), Config.INSTANCE.getFoojayMqttPort(), e.getMessage());
            }
        }

        if (!asyncClient.getState().isConnectedOrReconnect() && MqttClientState.CONNECTING != asyncClient.getState() && MqttClientState.CONNECTING_RECONNECT != asyncClient.getState()) {
            asyncClient.connectWith()
                       .cleanSession(cleanStart)
                       .keepAlive(30)
                       .simpleAuth()
                       .username(Config.INSTANCE.getFoojayMqttUser())
                       .password(UTF_8.encode(Config.INSTANCE.getFoojayMqttPassword()))
                       .applySimpleAuth()
                       .willPublish(Mqtt3Publish.builder()
                                                .topic(Constants.MQTT_LAST_WILL_TOPIC)
                                                .qos(MqttQos.EXACTLY_ONCE)
                                                .payload(Constants.MQTT_OFFLINE_MSG.getBytes(UTF_8))
                                                .build())
                       .send()
                       .whenComplete((connAck, throwable) -> {
                           if (throwable != null) {
                               // Handle failure to publish
                               LOGGER.error("Error connecting to mqtt broker {}", throwable.getMessage());
                           } else {
                               // Handle successful publish, e.g. logging or incrementing a metric
                               publish(Constants.MQTT_PRESENCE_TOPIC, MqttQos.EXACTLY_ONCE,true, Constants.MQTT_ONLINE_MSG);
                           }
                           connected.set(null == throwable);
                       });
        }
        asyncClient.publishes(ALL, publish -> {
            if (publish.getPayload().isPresent()) {
                fireMqttEvent(new MqttEvt(publish.getTopic().toString(), UTF_8.decode(publish.getPayload().get()).toString()));
            }
        });
    }

    private Mqtt3AsyncClient createAsyncClient() {
        Mqtt3AsyncClient asyncClient = MqttClient.builder()
                                                 .useMqttVersion3()
                                                 .serverHost(Config.INSTANCE.getFoojayMqttBroker())
                                                 .serverPort(Config.INSTANCE.getFoojayMqttPort())
                                                 .sslWithDefaultConfig()
                                                 .addDisconnectedListener(context -> {
                                                     context.getReconnector().reconnect(true).delay(10, TimeUnit.SECONDS);
                                                     LOGGER.debug("Try to reconnect because of: {}", context.getCause().getMessage());
                                                 })
                                                 .buildAsync();
        return asyncClient;
    }


    // ******************** Event Handling ************************************
    public void addMqttObserver(final MqttEvtObserver observer) {
        if (GHOST || observers.contains(observer)) { return; }
        observers.add(observer);
    }
    public void removeMqttObserver(final MqttEvtObserver observer) {
        if (GHOST) { return; }
        if (observers.contains(observer)) { observers.remove(observer); }
    }

    private void fireMqttEvent(final MqttEvt evt) {
        observers.forEach(observer -> observer.handleEvt(evt));
    }
}
