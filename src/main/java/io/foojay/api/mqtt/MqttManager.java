/*
 * Copyright (c) 2021.
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
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5WillPublish;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAck;
import eu.hansolo.properties.BooleanProperty;
import eu.hansolo.properties.ReadOnlyBooleanProperty;
import io.foojay.api.util.Config;
import io.foojay.api.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.hivemq.client.mqtt.MqttGlobalPublishFilter.ALL;
import static java.nio.charset.StandardCharsets.UTF_8;


public class MqttManager {
    private static final Logger                LOGGER = LoggerFactory.getLogger(MqttManager.class);
    private              Mqtt5AsyncClient      asyncClient;
    private              BooleanProperty       connected;
    private              List<MqttEvtObserver> observers;


    // ******************** Constructors **************************************
    public MqttManager() {
        this.connected = new BooleanProperty(MqttManager.this, "connected", false);
        this.observers = new CopyOnWriteArrayList<>();
        asyncClient    = MqttClient.builder()
                                   .useMqttVersion5()
                                   .serverHost(Config.INSTANCE.getFoojayMqttBroker())
                                   .serverPort(Config.INSTANCE.getFoojayMqttPort())
                                   .sslWithDefaultConfig()
                                   .automaticReconnectWithDefaultConfig()
                                   .buildAsync();

        init();
    }


    private void init() {
        connect(true);
    }


    // ******************** Methods *******************************************
    public boolean isConnected() { return connected.get(); }
    public ReadOnlyBooleanProperty connectedProperty() { return connected; }

    public CompletableFuture<Mqtt5PublishResult> publish(final String topic, final String msg) {
        return publish(topic, MqttQos.AT_LEAST_ONCE, false, msg);
    }
    public CompletableFuture<Mqtt5PublishResult> publish(final String topic, final MqttQos qos, final boolean retain, final String msg) {
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
                              } else {
                                  // Handle successful publish, e.g. logging or incrementing a metric
                              }
                          });
    }

    public CompletableFuture<Mqtt5SubAck> subscribe(final String topic, final MqttQos qos) {
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
        if (null == asyncClient || !asyncClient.getState().isConnected()) { connect(true); }
        asyncClient.unsubscribeWith().topicFilter(topic).send();
    }

    private void connect(final boolean cleanStart) {
        if (null == asyncClient) {
            asyncClient = MqttClient.builder()
                                    .useMqttVersion5()
                                    .serverHost(Config.INSTANCE.getFoojayMqttBroker())
                                    .serverPort(Config.INSTANCE.getFoojayMqttPort())
                                    .sslWithDefaultConfig()
                                    .automaticReconnectWithDefaultConfig()
                                    .buildAsync();
        }

        if (!asyncClient.getState().isConnected() && MqttClientState.CONNECTING != asyncClient.getState()) {
            asyncClient.connectWith()
                       .cleanStart(cleanStart)
                       .sessionExpiryInterval(0)
                       .keepAlive(30)
                       .simpleAuth()
                       .username(Config.INSTANCE.getFoojayMqttUser())
                       .password(UTF_8.encode(Config.INSTANCE.getFoojayMqttPassword()))
                       .applySimpleAuth()
                       .willPublish(Mqtt5WillPublish.builder()
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
        asyncClient.toAsync().publishes(ALL, publish -> {
            if (publish.getPayload().isPresent()) {
                fireMqttEvent(new MqttEvt(publish.getTopic().toString(), UTF_8.decode(publish.getPayload().get()).toString()));
            }
        });
    }


    // ******************** Event Handling ************************************
    public void addMqttObserver(final MqttEvtObserver observer) {
        if (observers.contains(observer)) { return; }
        observers.add(observer);
    }
    public void removeMqttObserver(final MqttEvtObserver observer) {
        if (observers.contains(observer)) { observers.remove(observer); }
    }

    private void fireMqttEvent(final MqttEvt evt) {
        observers.forEach(observer -> observer.handleEvt(evt));
    }
}
