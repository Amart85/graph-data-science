/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.gds.core.utils.paged;

public final class HugeDoubleArrayStack {

    private final HugeDoubleArray array;
    private final long capacity;
    private long size;

    public static HugeDoubleArrayStack newStack(long capacity) {
        return new HugeDoubleArrayStack(HugeDoubleArray.newArray(capacity));
    }

    private HugeDoubleArrayStack(HugeDoubleArray array) {
        this.capacity = array.size();
        this.array = array;
    }

    public void push(double v) {
        if (size == capacity) {
            throw new IndexOutOfBoundsException("Stack is full.");
        }
        array.set(size++, v);
    }

    public double pop() {
        if (isEmpty()) {
            throw new IndexOutOfBoundsException("Stack is empty.");
        }
        return array.get(--size);
    }

    public long size() {
        return size;
    }

    public boolean isEmpty() {
        return size() == 0;
    }
}
