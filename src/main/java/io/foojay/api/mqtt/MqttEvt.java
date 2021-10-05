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

import static io.foojay.api.util.Constants.COLON;
import static io.foojay.api.util.Constants.COMMA;
import static io.foojay.api.util.Constants.CURLY_BRACKET_CLOSE;
import static io.foojay.api.util.Constants.CURLY_BRACKET_OPEN;
import static io.foojay.api.util.Constants.QUOTES;


public class MqttEvt {
    private String topic;
    private String msg;


    // ******************** Constructors **************************************
    public MqttEvt(final String topic, final String msg) {
        this.topic = topic;
        this.msg   = msg;
    }


    // ******************** Messages ******************************************
    public String getTopic() { return topic; }

    public String getMsg() { return msg; }


    @Override public boolean equals(final Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        MqttEvt mqttEvt = (MqttEvt) o;

        if (!getTopic().equals(mqttEvt.getTopic())) { return false; }
        return getMsg().equals(mqttEvt.getMsg());
    }

    @Override public int hashCode() {
        int result = getTopic().hashCode();
        result = 31 * result + getMsg().hashCode();
        return result;
    }

    @Override public String toString() {
        return new StringBuilder().append(CURLY_BRACKET_OPEN)
                                  .append(QUOTES).append("topic").append(QUOTES).append(COLON).append(QUOTES).append(topic).append(QUOTES).append(COMMA)
                                  .append(QUOTES).append("msg").append(QUOTES).append(COLON).append(QUOTES).append(msg).append(QUOTES)
                                  .append(CURLY_BRACKET_CLOSE).toString();
    }
}
