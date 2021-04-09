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
import org.neo4j.graphalgo.core.utils.partition.Partition;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static org.neo4j.graphalgo.utils.StringFormatting.formatWithLocale;

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

    public enum Variant {
        NONE {
            @Override
            public ScalarScaler create(
                NodeProperties properties, long nodeCount, int concurrency, ExecutorService executor
            ) {
                return new ScalarScaler(properties) {
                    @Override
                    public double scaleProperty(long nodeId) {
                        return properties.doubleValue(nodeId);
                    }
                };
            }
        },
        MAX {
            @Override
            public ScalarScaler create(
                NodeProperties properties, long nodeCount, int concurrency, ExecutorService executor
            ) {
                return Max.create(properties, nodeCount, concurrency, executor);
            }
        },
        MINMAX {
            @Override
            public ScalarScaler create(
                NodeProperties properties, long nodeCount, int concurrency, ExecutorService executor
            ) {
                return MinMax.create(properties, nodeCount, concurrency, executor);
            }
        },
        MEAN {
            @Override
            public ScalarScaler create(
                NodeProperties properties, long nodeCount, int concurrency, ExecutorService executor
            ) {
                return Mean.create(properties, nodeCount, concurrency, executor);
            }
        },
        LOG {
            @Override
            public ScalarScaler create(
                NodeProperties properties, long nodeCount, int concurrency, ExecutorService executor
            ) {
                return LogTransformer.create(properties);
            }
        },
        STDSCORE {
            @Override
            public ScalarScaler create(
                NodeProperties properties, long nodeCount, int concurrency, ExecutorService executor
            ) {
                return StdScore.create(properties, nodeCount, concurrency, executor);
            }
        },
        L1NORM {
            @Override
            public ScalarScaler create(
                NodeProperties properties, long nodeCount, int concurrency, ExecutorService executor
            ) {
                return L1Norm.create(properties, nodeCount, concurrency, executor);
            }
        },
        L2NORM {
            @Override
            public ScalarScaler create(
                NodeProperties properties, long nodeCount, int concurrency, ExecutorService executor
            ) {
                return L2Norm.create(properties, nodeCount, concurrency, executor);
            }
        };

        public static Variant lookup(String name) {
            try {
                return valueOf(name.toUpperCase(Locale.ENGLISH));
            } catch (IllegalArgumentException e) {
                String availableStrategies = Arrays
                    .stream(values())
                    .map(Variant::name)
                    .collect(Collectors.joining(", "));
                throw new IllegalArgumentException(formatWithLocale(
                    "Scaler `%s` is not supported. Must be one of: %s.",
                    name,
                    availableStrategies
                ));
            }
        }

        public static String toString(Variant variant) {
            return variant.name();
        }

        /**
         * Create a scaler. Some scalers rely on aggregate extreme values which are computed at construction time.
         */
        public abstract ScalarScaler create(
            NodeProperties properties,
            long nodeCount,
            int concurrency,
            ExecutorService executor
        );
    }

    abstract static class AggregatesComputer implements Runnable {

        private final Partition partition;
        final NodeProperties properties;

        AggregatesComputer(Partition partition, NodeProperties property) {
            this.partition = partition;
            this.properties = property;
        }

        @Override
        public void run() {
            long end = partition.startNode() + partition.nodeCount();
            for (long nodeId = partition.startNode(); nodeId < end; nodeId++) {
                compute(nodeId);
            }
        }

        abstract void compute(long nodeId);
    }
}
