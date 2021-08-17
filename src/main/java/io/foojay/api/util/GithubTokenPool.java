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

package io.foojay.api.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public enum GithubTokenPool {
    INSTANCE;

    private static final CopyOnWriteArrayList<String> TOKEN_LIST = new CopyOnWriteArrayList<>(List.of());
    private static       int                          counter    = 0;

    public String next() {
        final String token = TOKEN_LIST.get(counter);
        counter++;
        if (counter > TOKEN_LIST.size() - 1) { counter = 0; }
        return token;
    }
}
