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

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class EphemeralIdCache<T extends String, U extends String> implements Cache<T, U> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EphemeralIdCache.class);

    private final ConcurrentHashMap<T, U> ephemeralIdCache = new ConcurrentHashMap<>(16, 0.9f, 1);

    @Override public void add(final T ephemeralId, final U pkgId) {
        if (null == ephemeralId) { return; }
        if (null == pkgId) {
            LOGGER.debug("EphemeralId cannot be null -> removed key {}", ephemeralId);
            ephemeralIdCache.remove(ephemeralId);
        } else {
            ephemeralIdCache.put(ephemeralId, pkgId);
        }
    }

    @Override public U get(final T ephemeralId) {
        if (null == ephemeralId || !ephemeralIdCache.containsKey(ephemeralId)) { return null; }
        return ephemeralIdCache.get(ephemeralId);
    }

    @Override public void remove(final T bundleInfoId) {
        ephemeralIdCache.remove(bundleInfoId);
    }
    @Override public void remove(final List<T> keysToRemove) { keysToRemove.forEach(key -> ephemeralIdCache.remove(key)); }

    @Override public void addAll(final Map<T,U> entries) { ephemeralIdCache.putAll(entries); }

    @Override public void clear() {
        ephemeralIdCache.clear();
        LOGGER.debug("EphemeralID cache cleared");
    }

    @Override public synchronized long size() {
        return ephemeralIdCache.size();
    }

    @Override public synchronized boolean isEmpty() { return ephemeralIdCache.isEmpty(); }

    public void setAll(final Map<T, U> entries) {
        ephemeralIdCache.clear();
        ephemeralIdCache.putAll(entries);
        LOGGER.debug("EphemeralID cache cleared and set with new values");
    }

    /**
     * Updates the cache with the values from the given patch map without updating
     * existing entries.
     * @param patch Map that contains existing and new entries
     */
    public void synchronize(final Map<T, U> patch) { patch.forEach(ephemeralIdCache::putIfAbsent); }

    /**
     * Updates the cache with the values from the given patch map including updates
     * of existing entries in cache. In addition entries that are in the cache but not
     * in the patch map can be removed if removeIfNotInPatch flag is true
     * @param patch
     * @param removeIfNotInPatch
     */
    public void update(final Map<T, U> patch, final boolean removeIfNotInPatch) {
            patch.forEach((key, value) -> ephemeralIdCache.merge(key, value, (v1, v2) -> v1.equals(v2) ? v1 : v2));
            if (removeIfNotInPatch) {
                if (ephemeralIdCache.size() > patch.size()) {
                    Map<T, U> toRemoveFromTarget = new HashMap<>();
                    ephemeralIdCache.entrySet().stream().filter(entry -> !patch.containsKey(entry.getKey())).forEach(entry -> toRemoveFromTarget.put(entry.getKey(), entry.getValue()));
                    toRemoveFromTarget.keySet().forEach(key -> ephemeralIdCache.remove(key));
                }
            }
        }

    public boolean containsEphemeralId(final T ephemeralId) { return ephemeralIdCache.containsKey(ephemeralId); }

    public T getEphemeralIdForPkgId(final U pkgId) {
        Optional<Entry<T, U>> optionalEntry = ephemeralIdCache.entrySet().stream().filter(entry -> entry.getValue().equals(pkgId)).findFirst();

        if (optionalEntry.isPresent()) {
            return optionalEntry.get().getKey();
        } else {
            final T ephemeralId = (T) Helper.createEphemeralId(Instant.now().getEpochSecond(), pkgId);
            add(ephemeralId, pkgId);
            return ephemeralId;
        }
    }

    public Set<Entry<T,U>> getEntrySet() { return ephemeralIdCache.entrySet(); }

    public Collection<T> getEphemeralIds() { return ephemeralIdCache.keySet(); }

    public Collection<U> getPkgIds() { return ephemeralIdCache.values(); }
}
