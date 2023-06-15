/*
 * Copyright (c) 2022.
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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public final class Partition<T> extends AbstractList<List<T>> {
    private final List<T> list;
    private final int     chunkSize;


    public Partition(final Collection<T> list, final int chunkSize) {
        this.list      = new ArrayList<>(list);
        this.chunkSize = chunkSize;
    }


    public static <T> Partition<T> ofSize(List<T> list, int chunkSize) { return new Partition<>(list, chunkSize); }


    @Override public List<T> get(int index) {
        final int start = index * chunkSize;
        final int end   = Math.min(start + chunkSize, list.size());

        if (start > end) { throw new IndexOutOfBoundsException("Index " + index + " is out of the list range <0," + (size() - 1) + ">"); }

        return new ArrayList<>(list.subList(start, end));
    }

    @Override public int size() { return (int) Math.ceil((double) list.size() / (double) chunkSize); }
}
