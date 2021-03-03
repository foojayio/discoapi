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
 * You should have received a copy of the GNU General Public License
 * along with DiscoAPI.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.foojay.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class EphemeralIdCache<T extends String, U extends String> implements Cache<T, U> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EphemeralIdCache.class);

    private final ConcurrentHashMap<T, U> ephemeralIdCache = new ConcurrentHashMap<>();

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

    @Override public void clear() {
        LOGGER.debug("EphemeralID cache cleared");
        ephemeralIdCache.clear();
    }

    @Override public long size() {
        return ephemeralIdCache.size();
    }

    @Override public boolean isEmpty() { return ephemeralIdCache.isEmpty(); }

    public boolean containsEphemeralId(final T ephemeralId) { return ephemeralIdCache.containsKey(ephemeralId); }

    public T getEphemeralIdForPkgId(final U pkgId) {
        Optional<Entry<T, U>> optionalEntry = ephemeralIdCache.entrySet().stream().filter(entry -> entry.getValue().equals(pkgId)).findFirst();
        return optionalEntry.isPresent() ? optionalEntry.get().getKey() : null;
    }

    public Set<Entry<T,U>> getEntrySet() { return ephemeralIdCache.entrySet(); }

    public Collection<T> getEphemeralIds() { return ephemeralIdCache.keySet(); }

    public Collection<U> getPkgIds() { return ephemeralIdCache.values(); }
}
