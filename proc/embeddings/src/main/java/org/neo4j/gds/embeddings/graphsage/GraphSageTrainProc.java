/*
 * Copyright (c) 2017-2020 "Neo4j,"
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
package org.neo4j.gds.embeddings.graphsage;

import org.neo4j.gds.embeddings.graphsage.algo.GraphSage;
import org.neo4j.gds.embeddings.graphsage.algo.GraphSageTrain;
import org.neo4j.gds.embeddings.graphsage.algo.GraphSageTrainAlgorithmFactory;
import org.neo4j.gds.embeddings.graphsage.algo.GraphSageTrainConfig;
import org.neo4j.graphalgo.AlgorithmFactory;
import org.neo4j.graphalgo.TrainProc;
import org.neo4j.graphalgo.api.GraphStore;
import org.neo4j.graphalgo.config.GraphCreateConfig;
import org.neo4j.graphalgo.core.CypherMapWrapper;
import org.neo4j.graphalgo.core.loading.GraphStoreWithConfig;
import org.neo4j.graphalgo.core.model.Model;
import org.neo4j.graphalgo.core.model.ModelCatalog;
import org.neo4j.graphalgo.results.MemoryEstimateResult;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.neo4j.gds.embeddings.graphsage.GraphSageCompanion.GRAPHSAGE_DESCRIPTION;

public class GraphSageTrainProc extends TrainProc<GraphSageTrain, ModelData, GraphSageTrainConfig> {

    @Description(GRAPHSAGE_DESCRIPTION)
    @Procedure(name = "gds.beta.graphSage.train", mode = Mode.READ)
    public Stream<TrainResult> train(
        @Name(value = "graphName") Object graphNameOrConfig,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        ModelCatalog.checkStorable(username(), GraphSage.MODEL_TYPE);

        ComputationResult<GraphSageTrain, Model<ModelData, GraphSageTrainConfig>, GraphSageTrainConfig> computationResult = compute(
            graphNameOrConfig,
            configuration
        );
        Model<ModelData, GraphSageTrainConfig> result = computationResult.result();

        ModelCatalog.set(result);
        return Stream.of(trainResult(computationResult));
    }

    @Description(ESTIMATE_DESCRIPTION)
    @Procedure(name = "gds.beta.graphSage.train.estimate", mode = Mode.READ)
    public Stream<MemoryEstimateResult> estimate(
        @Name(value = "graphName") Object graphNameOrConfig,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        return computeEstimate(graphNameOrConfig, configuration);
    }

    @Override
    protected GraphSageTrainConfig newConfig(
        String username,
        Optional<String> graphName,
        Optional<GraphCreateConfig> maybeImplicitCreate,
        CypherMapWrapper config
    ) {
        return GraphSageTrainConfig.of(username, graphName, maybeImplicitCreate, config);
    }

    @Override
    protected AlgorithmFactory<GraphSageTrain, GraphSageTrainConfig> algorithmFactory() {
        return new GraphSageTrainAlgorithmFactory();
    }

    @Override
    protected void validateConfigsAndGraphStore(
        GraphStoreWithConfig graphStoreWithConfig,
        GraphSageTrainConfig config
    ) {
        config.validateAgainstGraphStore(graphStoreWithConfig);
    }

    @Override
    protected void validateGraphStore(GraphStore graphStore) {
        if (graphStore.relationshipCount() == 0) {
            throw new IllegalArgumentException("There should be at least one relationship in the graph.");
        }
    }
}
