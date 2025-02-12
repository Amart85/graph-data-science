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
package org.neo4j.gds.leiden;

import org.neo4j.gds.core.utils.paged.HugeDoubleArray;
import org.neo4j.gds.core.utils.paged.HugeLongArray;

class Partition {
    private final HugeLongArray communities;
    private final HugeDoubleArray communityVolumes;
    private final long communityCount;
    private final double modularity;

    Partition(HugeLongArray communities, HugeDoubleArray communityVolumes, long communityCount, double modularity) {
        this.communities = communities;
        this.communityVolumes = communityVolumes;
        this.communityCount = communityCount;
        this.modularity = modularity;
    }

    HugeLongArray communities() {
        return communities;
    }

    HugeDoubleArray communityVolumes() {
        return communityVolumes;
    }

    long communityCount() {
        return communityCount;
    }

    double modularity() {
        return modularity;
    }
}
