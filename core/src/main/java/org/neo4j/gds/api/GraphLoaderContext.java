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
package org.neo4j.gds.api;

import org.immutables.value.Value;
import org.neo4j.gds.annotation.ValueClass;
import org.neo4j.gds.core.concurrency.Pools;
import org.neo4j.gds.core.utils.TerminationFlag;
import org.neo4j.gds.core.utils.progress.EmptyTaskRegistryFactory;
import org.neo4j.gds.core.utils.progress.TaskRegistryFactory;
import org.neo4j.gds.core.utils.warnings.EmptyUserLogRegistryFactory;
import org.neo4j.gds.core.utils.warnings.UserLogRegistryFactory;
import org.neo4j.gds.transaction.TransactionContext;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.logging.Log;
import org.neo4j.logging.NullLog;

import java.util.concurrent.ExecutorService;

@ValueClass
public interface GraphLoaderContext {

    TransactionContext transactionContext();

    GraphDatabaseService graphDatabaseService();

    Log log();

    @Value.Default
    default ExecutorService executor() {
        return Pools.DEFAULT;
    }

    @Value.Default
    default TerminationFlag terminationFlag() {
        return TerminationFlag.RUNNING_TRUE;
    }

    TaskRegistryFactory taskRegistryFactory();

    UserLogRegistryFactory userLogRegistryFactory();


    GraphLoaderContext NULL_CONTEXT = new GraphLoaderContext() {
        @Override
        public TransactionContext transactionContext() {
            return null;
        }

        @Override
        public GraphDatabaseService graphDatabaseService() {
            return null;
        }

        @Override
        public Log log() {
            return NullLog.getInstance();
        }

        @Override
        public TaskRegistryFactory taskRegistryFactory() {
            return EmptyTaskRegistryFactory.INSTANCE;
        }

        @Override
        public UserLogRegistryFactory userLogRegistryFactory() {
            return EmptyUserLogRegistryFactory.INSTANCE;
        }
    };
}
