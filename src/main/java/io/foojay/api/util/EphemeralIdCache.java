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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


public class EphemeralIdCache<T extends String, U extends String> implements Cache<T, U> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EphemeralIdCache.class);

    private final ConcurrentHashMap<T, U> bundleInfoCache = new ConcurrentHashMap<>();

    @Override public void add(final T bundleInfoId, final U id) {
        if (null == bundleInfoId) { return; }
        if (null == id) {
            LOGGER.debug("bundleInfoId cannot be null -> removed key {}", bundleInfoId);
            bundleInfoCache.remove(bundleInfoId);
        } else {
            bundleInfoCache.put(bundleInfoId, id);
        }
    }

    @Override public U get(final T bundleInfoId) {
        if (null == bundleInfoId || !bundleInfoCache.containsKey(bundleInfoId)) { return null; }
        return bundleInfoCache.get(bundleInfoId);
    }

    @Override public void remove(final T bundleInfoId) {
        bundleInfoCache.remove(bundleInfoId);
    }

    @Override public void clear() {
        LOGGER.debug("BundleInfo cache cleared");
        bundleInfoCache.clear();
    }

    @Override public long size() {
        return bundleInfoCache.size();
    }

    @Override public boolean isEmpty() { return bundleInfoCache.isEmpty(); }

    public boolean containsEphemeralId(final T bundleInfoId) { return bundleInfoCache.containsKey(bundleInfoId); }

    public T getEphemeralIdForPkgId(final U id) {
        Optional<Entry<T, U>> optionalEntry = bundleInfoCache.entrySet().stream().filter(entry -> entry.getValue().equals(id)).findFirst();
        return optionalEntry.isPresent() ? optionalEntry.get().getKey() : null;
    }

    public Collection<T> getBundleInfoIds() { return bundleInfoCache.keySet(); }

    public Collection<U> getIds() { return bundleInfoCache.values(); }
}
