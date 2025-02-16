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
package org.neo4j.gds.betweenness;

import org.jetbrains.annotations.Nullable;
import org.neo4j.gds.GraphAlgorithmFactory;
import org.neo4j.gds.MutatePropertyProc;
import org.neo4j.gds.api.properties.nodes.NodePropertyValues;
import org.neo4j.gds.core.CypherMapWrapper;
import org.neo4j.gds.core.utils.paged.HugeAtomicDoubleArray;
import org.neo4j.gds.executor.ComputationResult;
import org.neo4j.gds.executor.ExecutionContext;
import org.neo4j.gds.executor.GdsCallable;
import org.neo4j.gds.executor.validation.ValidationConfiguration;
import org.neo4j.gds.result.AbstractCentralityResultBuilder;
import org.neo4j.gds.result.AbstractResultBuilder;
import org.neo4j.gds.results.MemoryEstimateResult;
import org.neo4j.internal.kernel.api.procs.ProcedureCallContext;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;

import static org.neo4j.gds.executor.ExecutionMode.MUTATE_NODE_PROPERTY;
import static org.neo4j.procedure.Mode.READ;

@GdsCallable(name = "gds.betweenness.mutate", description = BetweennessCentralityProc.BETWEENNESS_DESCRIPTION, executionMode = MUTATE_NODE_PROPERTY)
public class BetweennessCentralityMutateProc extends MutatePropertyProc<BetweennessCentrality, HugeAtomicDoubleArray, BetweennessCentralityMutateProc.MutateResult, BetweennessCentralityMutateConfig> {

    @Procedure(value = "gds.betweenness.mutate", mode = READ)
    @Description(BetweennessCentralityProc.BETWEENNESS_DESCRIPTION)
    public Stream<MutateResult> mutate(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        return mutate(compute(graphName, configuration));
    }

    @Procedure(value = "gds.betweenness.mutate.estimate", mode = READ)
    @Description(BetweennessCentralityProc.BETWEENNESS_DESCRIPTION)
    public Stream<MemoryEstimateResult> estimate(
        @Name(value = "graphNameOrConfiguration") Object graphNameOrConfiguration,
        @Name(value = "algoConfiguration") Map<String, Object> algoConfiguration
    ) {
        return computeEstimate(graphNameOrConfiguration, algoConfiguration);
    }

    @Override
    protected BetweennessCentralityMutateConfig newConfig(String username, CypherMapWrapper config) {
        return BetweennessCentralityMutateConfig.of(config);
    }

    @Override
    public ValidationConfiguration<BetweennessCentralityMutateConfig> validationConfig() {
        return BetweennessCentralityProc.getValidationConfig();
    }

    @Override
    public GraphAlgorithmFactory<BetweennessCentrality, BetweennessCentralityMutateConfig> algorithmFactory() {
        return BetweennessCentralityProc.algorithmFactory();
    }

    @Override
    protected NodePropertyValues nodeProperties(ComputationResult<BetweennessCentrality, HugeAtomicDoubleArray, BetweennessCentralityMutateConfig> computationResult) {
        return BetweennessCentralityProc.nodeProperties(computationResult);
    }

    @Override
    protected AbstractResultBuilder<MutateResult> resultBuilder(
        ComputationResult<BetweennessCentrality, HugeAtomicDoubleArray, BetweennessCentralityMutateConfig> computeResult,
        ExecutionContext executionContext
    ) {
        return BetweennessCentralityProc.resultBuilder(new MutateResult.Builder(
            executionContext.callContext(),
            computeResult.config().concurrency()
        ), computeResult);
    }

    @SuppressWarnings("unused")
    public static final class MutateResult extends BetweennessCentralityStatsProc.StatsResult {

        public final long nodePropertiesWritten;
        public final long mutateMillis;

        MutateResult(
            long nodePropertiesWritten,
            long preProcessingMillis,
            long computeMillis,
            long postProcessingMillis,
            long mutateMillis,
            @Nullable Map<String, Object> centralityDistribution,

            Map<String, Object> config
        ) {
            super(
                centralityDistribution,
                preProcessingMillis,
                computeMillis,
                postProcessingMillis,
                config
            );
            this.nodePropertiesWritten = nodePropertiesWritten;
            this.mutateMillis = mutateMillis;
        }

        static final class Builder extends AbstractCentralityResultBuilder<MutateResult> {

            Builder(ProcedureCallContext context, int concurrency) {
                super(context, concurrency);
            }

            @Override
            public MutateResult buildResult() {
                return new MutateResult(
                    nodePropertiesWritten,
                    preProcessingMillis,
                    computeMillis,
                    postProcessingMillis,
                    mutateMillis,
                    centralityHistogram,
                    config.toMap()
                );
            }
        }
    }
}
