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
package org.neo4j.gds;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.gds.core.CypherMapWrapper;
import org.neo4j.gds.test.config.RelationshipWeightConfigProcTest;
import org.neo4j.gds.traversal.RandomWalk;
import org.neo4j.gds.traversal.RandomWalkStreamConfig;
import org.neo4j.gds.walking.RandomWalkStreamProc;
import org.neo4j.graphdb.Path;
import org.neo4j.kernel.internal.GraphDatabaseAPI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@SuppressWarnings("unchecked")
class RandomWalkProcTest extends BaseProcTest implements AlgoBaseProcTest<RandomWalk, RandomWalkStreamConfig, Stream<long[]>> {

    private static final String DB_CYPHER =
        "CREATE" +
        "  (a:Node1)" +
        ", (b:Node1)" +
        ", (c:Node2)" +
        ", (d:Isolated)" +
        ", (e:Isolated)" +
        ", (a)-[:REL1]->(b)" +
        ", (b)-[:REL1]->(a)" +
        ", (a)-[:REL1]->(c)" +
        ", (c)-[:REL2]->(a)" +
        ", (b)-[:REL2]->(c)" +
        ", (c)-[:REL2]->(b)";

    @BeforeEach
    void setup() throws Exception {
        registerProcedures(RandomWalkStreamProc.class);
        runQuery(DB_CYPHER);
    }

    @Test
    void shouldRunSimpleConfig() {
        String query = GdsCypher.call()
            .loadEverything(Orientation.UNDIRECTED)
            .algo("gds", "alpha", "randomWalk")
            .streamMode()
            .addParameter("walksPerNode", 3)
            .addParameter("walkLength", 10)
            .yields();

        Collection<List<Long>> result = new ArrayList<>();

        runQueryWithRowConsumer(query, row -> result.add((List<Long>) row.get("nodeIds")));

        int expectedNumberOfWalks = 3 * 3;
        assertEquals(expectedNumberOfWalks, result.size());
        List<Long> walkForNodeZero = result
            .stream()
            .filter(arr -> arr.get(0) == 0)
            .findFirst()
            .orElse(List.of());
        assertEquals(10, walkForNodeZero.size());
    }

    @Test
    void shouldReturnPath() {
        String query = GdsCypher.call()
            .loadEverything(Orientation.UNDIRECTED)
            .algo("gds", "alpha", "randomWalk")
            .streamMode()
            .addParameter("walksPerNode", 3)
            .addParameter("walkLength", 10)
            .addParameter("returnPath", true)
            .yields();

        List<List<Long>> nodeIds = new ArrayList<>();
        List<Path> paths = new ArrayList<>();

        runQueryWithRowConsumer(query, row -> {
            nodeIds.add((List<Long>) row.get("nodeIds"));
            paths.add((Path) row.get("path"));
        });

        for (int i = 0; i < paths.size(); i++) {
            var nodes = nodeIds.get(i);
            var path = paths.get(i);

            AtomicInteger indexInPath = new AtomicInteger(0);
            path.nodes().forEach(node -> assertEquals(node.getId(), nodes.get(indexInPath.getAndIncrement())));
        }
    }

    static Stream<Arguments> sourceNodes() {
        return Stream.of(
            arguments(List.of(0L, 1L), List.of(0L, 1L)),
            arguments(List.of(0L), 0L)
        );
    }

    @ParameterizedTest
    @MethodSource("sourceNodes")
    void shouldParseSourceNodeIds(Collection<Long> parsedSourceNodes, Object rawSourceNodes) {
        String query = GdsCypher.call()
            .loadEverything(Orientation.UNDIRECTED)
            .algo("gds", "alpha", "randomWalk")
            .streamMode()
            .addParameter("walksPerNode", 1)
            .addParameter("walkLength", 3)
            .addParameter("returnPath", true)
            .addParameter("sourceNodes", rawSourceNodes)
            .yields();

        List<List<Long>> nodeIds = new ArrayList<>();
        runQueryWithRowConsumer(query, row -> nodeIds.add((List<Long>) row.get("nodeIds")));

        assertThat(nodeIds).matches(walks -> walks.size() == parsedSourceNodes.size());
        for (long sourceNode : parsedSourceNodes) {
            assertThat(nodeIds).anyMatch(walk -> walk.get(0) == sourceNode);
        }
    }

    @Override
    public Class<? extends AlgoBaseProc<RandomWalk, Stream<long[]>, RandomWalkStreamConfig>> getProcedureClazz() {
        return RandomWalkStreamProc.class;
    }

    @Override
    public GraphDatabaseAPI graphDb() {
        return db;
    }

    @Override
    public RandomWalkStreamConfig createConfig(CypherMapWrapper mapWrapper) {
        return RandomWalkStreamConfig.of("", Optional.empty(), Optional.empty(), mapWrapper);
    }

    @Override
    public void assertResultEquals(Stream<long[]> result1, Stream<long[]> result2) {
        var resultList1 = result1.collect(Collectors.toList());
        var resultList2 = result2.collect(Collectors.toList());

        for (int i = 0; i < resultList1.size(); i++) {
            var path1 = resultList1.get(i);
            var path2 = resultList2.get(i);
            assertThat(path1.length).isEqualTo(path2.length);
        }
    }

    @Override
    public boolean releaseAlgorithm() {
        return false;
    }

    @TestFactory
    Stream<DynamicTest> configTests() {
        return Stream.of(
            RelationshipWeightConfigProcTest.allTheTests(proc(), createMinimalConfig())
        ).flatMap(Collection::stream);
    }
}
