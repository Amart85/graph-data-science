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
package org.neo4j.gds.impl.traverse;

import com.carrotsearch.hppc.DoubleArrayList;
import com.carrotsearch.hppc.LongArrayList;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.core.utils.TerminationFlag;
import org.neo4j.gds.core.utils.paged.HugeAtomicBitSet;
import org.neo4j.gds.core.utils.paged.HugeAtomicLongArray;
import org.neo4j.gds.core.utils.paged.HugeDoubleArray;
import org.neo4j.gds.core.utils.paged.HugeLongArray;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.neo4j.gds.impl.Converters.longToIntConsumer;

class BFSTask implements Runnable {
    // shared variables
    private final Graph graph;
    private final AtomicInteger traversedNodesIndex;
    private final HugeAtomicBitSet visited;
    private final HugeLongArray traversedNodes;
    private final HugeLongArray sources;
    private final HugeDoubleArray weights;
    private final AtomicLong targetFoundIndex;
    private final AtomicInteger traversedNodesLength;
    private final HugeAtomicLongArray minimumChunk;
    private final ExitPredicate exitPredicate;
    private final Aggregator aggregatorFunction;

    // variables local to the task
    private final LongArrayList localNodes;
    private final DoubleArrayList localWeights;
    private final LongArrayList chunks;

    private final int delta;
    private final TerminationFlag terminationFlag;
    private int indexOfChunk;
    private int indexOfLocalNodes;

    BFSTask(
        Graph graph,
        HugeLongArray traversedNodes,
        AtomicInteger traversedNodesIndex,
        AtomicInteger traversedNodesLength,
        HugeAtomicBitSet visited,
        HugeLongArray sources,
        HugeDoubleArray weights,
        AtomicLong targetFoundIndex,
        HugeAtomicLongArray minimumChunk,
        ExitPredicate exitPredicate,
        Aggregator aggregatorFunction,
        int delta,
        TerminationFlag terminationFlag
    ) {
        this.graph = graph.concurrentCopy();
        this.traversedNodesIndex = traversedNodesIndex;
        this.traversedNodesLength = traversedNodesLength;
        this.visited = visited;
        this.traversedNodes = traversedNodes;
        this.sources = sources;
        this.weights = weights;
        this.targetFoundIndex = targetFoundIndex;
        this.minimumChunk = minimumChunk;
        this.exitPredicate = exitPredicate;
        this.aggregatorFunction = aggregatorFunction;
        this.delta = delta;
        this.terminationFlag = terminationFlag;

        this.localNodes = new LongArrayList();
        this.localWeights = new DoubleArrayList();
        this.chunks = new LongArrayList();
    }

    long currentChunkId() {
        return this.chunks.get(indexOfChunk);
    }

    private void moveToNextChunk() {
        indexOfChunk++;
    }

    boolean hasMoreChunks() {
        return indexOfChunk < chunks.size();
    }

    public void run() {
        resetChunks();

        int offset;
        while ((offset = traversedNodesIndex.getAndAdd(delta)) < traversedNodesLength.get()) {
            int chunkLimit = Math.min(offset + delta, traversedNodesLength.get());
            chunks.add(offset);
            for (int idx = offset; idx < chunkLimit; idx++) {
                var nodeId = traversedNodes.get(idx);
                var sourceId = sources.get(idx);
                var weight = weights.get(idx);
                relaxNode(offset, idx, nodeId, sourceId, weight);
            }
            localNodes.add(BFS.IGNORE_NODE);
            localWeights.add(BFS.IGNORE_NODE);
        }
    }

    void syncNextChunk() {
        if (!localNodes.isEmpty()) {
            int nodesTraversed = 0;
            int index = traversedNodesLength.get();
            while (localNodes.get(indexOfLocalNodes) != BFS.IGNORE_NODE) {
                long nodeId = localNodes.get(indexOfLocalNodes);
                long minimumChunkIndex = minimumChunk.get(nodeId);
                if (minimumChunkIndex != BFS.IGNORE_NODE && minimumChunkIndex < delta + currentChunkId()) {
                    traversedNodes.set(index, nodeId);
                    minimumChunk.set(nodeId, BFS.IGNORE_NODE);
                    sources.set(index, traversedNodes.get(minimumChunkIndex));
                    weights.set(index, localWeights.get(indexOfLocalNodes));
                    visited.set(nodeId);
                    index++;
                    nodesTraversed++;
                }
                indexOfLocalNodes++;
            }
            indexOfLocalNodes++;
            traversedNodesLength.getAndAdd(nodesTraversed);
            moveToNextChunk();
            if (!hasMoreChunks()) {
                resetLocalState();
            }
        }
    }

    private void relaxNode(int chunk, int nodeIndex, long nodeId, long sourceNodeId, double weight) {
        var exitPredicateResult = exitPredicate.test(sourceNodeId, nodeId, weight);
        if (exitPredicateResult == ExitPredicate.Result.CONTINUE) {
            traversedNodes.set(nodeIndex, BFS.IGNORE_NODE);
            return;
        } else {
            if (exitPredicateResult == ExitPredicate.Result.BREAK) {
                targetFoundIndex.getAndAccumulate(nodeIndex, Math::min);
                return;
            }
        }

        this.graph.forEachRelationship(
            nodeId,
            longToIntConsumer((s, t) -> {
                // Potential race condition
                // We consider it okay since it's handled by `minimumChunk.update`
                // which is an atomic operation and always will have the minimum chunk index
                if (!visited.get(t)) {
                    minimumChunk.update(t, r -> Math.min(r, nodeIndex));
                    if (minimumChunk.get(t) == nodeIndex) {
                        localNodes.add(t);
                        localWeights.add(aggregatorFunction.apply(s, t, weight));
                    }
                }
                return terminationFlag.running();
            })
        );
    }

    private void resetLocalState() {
        localNodes.elementsCount = 0;
        localWeights.elementsCount = 0;
        indexOfLocalNodes = 0;
    }

    private void resetChunks() {
        chunks.elementsCount = 0;
        indexOfChunk = 0;
    }
}
