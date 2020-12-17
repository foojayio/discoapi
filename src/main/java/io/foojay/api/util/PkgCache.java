/*
 * Copyright (c) 2020.
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

import io.foojay.api.pkg.Pkg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;


public class PkgCache<T extends String, U extends Pkg> implements Cache<T, U> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PkgCache.class);

    private final ConcurrentHashMap<T, U> cache = new ConcurrentHashMap<>();

    @Override public void add(final T key, final U pkg) {
        if (null == key) { return; }
        if (null == pkg) {
            LOGGER.debug("Package cannot be null -> removed key {}", key);
            cache.remove(key);
        } else {
            cache.put(key, pkg);
        }
    }

    @Override public U get(final T key) {
        if (null == key || !cache.containsKey(key)) { return null; }
        return cache.get(key);
    }

    @Override public void remove(final T key) {
        cache.remove(key);
    }

    @Override public void clear() {
        LOGGER.debug("Package cache cleared");
        cache.clear();
    }

    @Override public long size() {
        return cache.size();
    }

    @Override public boolean isEmpty() { return cache.isEmpty(); }

    public boolean containsKey(final T key) { return cache.containsKey(key); }

    public Collection<T> getKeys() { return cache.keySet(); }

    public Collection<U> getPkgs() { return cache.values(); }
}

