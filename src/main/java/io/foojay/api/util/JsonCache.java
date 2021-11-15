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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class JsonCache<T extends String, U extends String> implements Cache<T, U> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonCache.class);

    private final ConcurrentHashMap<T, U> cache = new ConcurrentHashMap<>(16, 0.9f, 1);


    @Override public void add(final T key, final U json) {
        if (null == key) { return; }
        if (null == json) {
            LOGGER.debug("Package cannot be null -> removed key {}", key);
            cache.remove(key);
        } else {
            cache.put(key, json);
        }
    }

    @Override public U get(final T key) {
        return cache.get(key);
    }

    @Override public void remove(final T key) {
        cache.remove(key);
    }
    @Override public void remove(final List<T> keysToRemove) { keysToRemove.forEach(key -> cache.remove(key)); }

    @Override public synchronized void addAll(final Map<T,U> entries) {
        cache.putAll(entries);
    }

    @Override public synchronized void clear() {
        cache.clear();
        LOGGER.debug("JSON cache cleared");
    }

    @Override public synchronized long size() {
        return cache.size();
    }

    @Override public synchronized boolean isEmpty() { return cache.isEmpty(); }

    public void putIfAbsent(final T key, final U json) { cache.putIfAbsent(key, json); }

    public void put(final T key, final U json) { cache.put(key, json); }

    /**
     * Replaces all entries in the cache with the ones in the given patch
     * @param patch
     */
    public void setAll(final Map<T,U> patch) {
        synchronized (cache) {
            cache.clear();
            cache.putAll(patch);
            LOGGER.debug("JSON cache cleared and set with new data");
        }
    }

    /**
     * Updates the cache with the values from the given patch map without updating
     * existing entries.
     * @param patch Map that contains existing and new entries
     */
    public void synchronize(final Map<T, U> patch) { patch.forEach(cache::putIfAbsent); }

    /**
     * Updates the cache with the values from the given patch map including updates
     * of existing entries in cache. In addition entries that are in the cache but not
     * in the patch map can be removed if removeIfNotInPatch flag is true
     * @param patch
     * @param removeIfNotInPatch
     */
    public void update(final Map<T, U> patch, final boolean removeIfNotInPatch) {
        patch.forEach((key, value) -> cache.merge(key, value, (v1, v2) -> v1.equals(v2) ? v1 : v2));
        if (removeIfNotInPatch) {
            if (cache.size() > patch.size()) {
                Map<T, U> toRemoveFromTarget = new HashMap<>();
                cache.entrySet().stream().filter(entry -> !patch.containsKey(entry.getKey())).forEach(entry -> toRemoveFromTarget.put(entry.getKey(), entry.getValue()));
                toRemoveFromTarget.keySet().forEach(key -> cache.remove(key));
            }
        }
    }

    /**
     * Replaces all entries in the cache with values from the given patch. In addition
     * it removes entries which does not exist in the patch but in the cache.
     * @param patch
     * @param removeIfNotInPatch
     */
    public void replace(final Map<T, U> patch, final boolean removeIfNotInPatch) {
        patch.forEach((key, value) -> cache.replace(key, value));
        if (removeIfNotInPatch) {
            if (cache.size() > patch.size()) {
                Map<T, U> toRemoveFromTarget = new HashMap<>();
                cache.entrySet().stream().filter(entry -> !patch.containsKey(entry.getKey())).forEach(entry -> toRemoveFromTarget.put(entry.getKey(), entry.getValue()));
                toRemoveFromTarget.keySet().forEach(key -> cache.remove(key));
            }
        }
    }

    public boolean containsKey(final T key) { return cache.containsKey(key); }

    public Set<Entry<T,U>> getEntrySet() { return cache.entrySet(); }

    public Collection<T> getKeys() { return cache.keySet(); }

    public Collection<U> getValues() { return new ArrayList<>(cache.values()); }

    /**
     * Returns a shallow copy of the cache
     * @return a shallow copy of the cache
     */
    public ConcurrentHashMap<T,U> getCopy() { return new ConcurrentHashMap<>(cache); }

    /**
     * Returns a deep copy of the cache
     * @return a deep copy of the cache
     */
    public ConcurrentHashMap<String, String> getDeepCopy() {
        ConcurrentHashMap<String, String> deepCopy = new ConcurrentHashMap<>(16, 0.6f, 1);
        for (Entry<T,U> entry : cache.entrySet()) {
            String id   = new String(entry.getKey());
            String json = new String(entry.getValue());
            deepCopy.put(id, json);
        }
        return deepCopy;
    }
}
