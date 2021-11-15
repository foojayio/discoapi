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

package io.foojay.api;

import io.foojay.api.util.State;

import java.time.Instant;

import static io.foojay.api.util.Constants.COLON;
import static io.foojay.api.util.Constants.COMMA;
import static io.foojay.api.util.Constants.CURLY_BRACKET_CLOSE;
import static io.foojay.api.util.Constants.CURLY_BRACKET_OPEN;
import static io.foojay.api.util.Constants.QUOTES;


public enum StateManager {
    INSTANCE;

    private State  state;
    private String msg;
    private long   timestamp;


    StateManager() {
        this.state     = State.IDLE;
        this.msg       = "nominal";
        this.timestamp = Instant.now().getEpochSecond();
    }


    public State getState()      { return state; }
    public String getMsg()       { return msg;   }
    public long   getTimestamp() { return timestamp; }

    public void setState(final State state) {
        setState(state, "");
    }
    public void setState(final State state, final String msg) {
        this.timestamp = Instant.now().getEpochSecond();
        this.state     = state;
        this.msg       = msg;
    }

    @Override public String toString() {
        return new StringBuilder().append(CURLY_BRACKET_OPEN)
                                  .append(QUOTES).append("state").append(QUOTES).append(COLON).append(QUOTES).append(state.name()).append(QUOTES).append(COMMA)
                                  .append(QUOTES).append("msg").append(QUOTES).append(COLON).append(QUOTES).append(msg).append(QUOTES).append(COMMA)
                                  .append(QUOTES).append("timestamp").append(QUOTES).append(COLON).append(timestamp)
                                  .append(CURLY_BRACKET_CLOSE)
                                  .toString();
    }
}
