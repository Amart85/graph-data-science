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
package org.neo4j.gds.core.utils.collection.primitive;

import org.junit.jupiter.api.Test;

import java.util.PrimitiveIterator;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrimitiveLongCollectionsTest {

    @Test
    void shouldNotContinueToCallNextOnHasNextFalse() {
        // GIVEN
        AtomicLong count = new AtomicLong(2);
        PrimitiveIterator.OfLong iterator = new PrimitiveLongCollections.PrimitiveLongBaseIterator() {
            @Override
            protected boolean fetchNext() {
                return count.decrementAndGet() >= 0 && next(count.get());
            }
        };

        // WHEN/THEN
        assertTrue(iterator.hasNext());
        assertTrue(iterator.hasNext());
        assertEquals(1L, iterator.nextLong());
        assertTrue(iterator.hasNext());
        assertTrue(iterator.hasNext());
        assertEquals(0L, iterator.nextLong());
        assertFalse(iterator.hasNext());
        assertFalse(iterator.hasNext());
        assertEquals(-1L, count.get());
    }
}
