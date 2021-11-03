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

import io.foojay.api.pkg.Pkg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class PkgCache<T extends String, U extends Pkg> implements Cache<T, U> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PkgCache.class);

    private final ConcurrentHashMap<T, U> cache = new ConcurrentHashMap<>(16, 0.9f, 1);

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
        return cache.get(key);
    }

    @Override public void remove(final T key) {
        cache.remove(key);
    }

    @Override public void addAll(final Map<T,U> entries) {
        synchronized (cache) {
            cache.putAll(entries);
        }
    }

    @Override public void clear() {
        synchronized (cache) {
            cache.clear();
            LOGGER.debug("Package cache cleared");
        }
    }

    @Override public long size() {
        return cache.size();
    }

    @Override public boolean isEmpty() { return cache.isEmpty(); }

    public void setAll(final Map<T,U> entries) {
        synchronized (cache) {
            cache.clear();
            cache.putAll(entries);
            LOGGER.debug("Package cache cleared and set with new data");
        }
    }

    /**
     * Updates the cache with the values from the given patch map without updating
     * existing entries.
     * @param patch Map that contains existing and new entries
     */
    public void synchronize(final Map<T, U> patch) {
        synchronized (cache) {
            patch.forEach(cache::putIfAbsent);
        }
    }

    /**
     * Updates the cache with the values from the given patch map including updates
     * of existing entries in cache. In addition entries that are in the cache but not
     * in the patch map can be removed if removeIfNotInPatch flag is true
     * @param patch
     * @param removeIfNotInPatch
     */
    public void update(final Map<T, U> patch, final boolean removeIfNotInPatch) {
        synchronized (cache) {
            patch.forEach((key, value) -> cache.merge(key, value, (v1, v2) -> v1.equals(v2) ? v1 : v2));
            if (removeIfNotInPatch) {
                if (cache.size() > patch.size()) {
                    Map<T, U> toRemoveFromTarget = new HashMap<>();
                    cache.entrySet().stream().filter(entry -> !patch.containsKey(entry.getKey())).forEach(entry -> toRemoveFromTarget.put(entry.getKey(), entry.getValue()));
                    toRemoveFromTarget.keySet().forEach(key -> cache.remove(key));
                }
            }
        }
    }

    public boolean containsKey(final T key) { return cache.containsKey(key); }

    public Set<Entry<T,U>> getEntrySet() { return cache.entrySet(); }

    public Collection<T> getKeys() { return cache.keySet(); }

    public Collection<U> getPkgs() { return cache.values(); }

    public HashMap<T,U> getCopy() { return new HashMap<>(cache); }
}

