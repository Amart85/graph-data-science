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
package org.neo4j.gds.scaling;

import org.neo4j.graphalgo.api.NodeProperties;

public abstract class ScalarScaler implements Scaler {

    protected final NodeProperties properties;

    public ScalarScaler(NodeProperties properties) {this.properties = properties;}

    public abstract double scaleProperty(long nodeId);

    @Override
    public void scaleProperty(long nodeId, double[] result, int offset) {
        result[offset] = scaleProperty(nodeId);
    }

    @Override
    public int dimension() {
        return 1;
    }

    public static final org.neo4j.gds.scaling.ScalarScaler ZERO = new org.neo4j.gds.scaling.ScalarScaler(null) {
        @Override
        public double scaleProperty(long nodeId) {
            return 0;
        }
    };
}
