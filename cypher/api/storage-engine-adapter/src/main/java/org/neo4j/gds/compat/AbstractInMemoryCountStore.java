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
package org.neo4j.gds.compat;

import org.neo4j.annotations.documented.ReporterFactory;
import org.neo4j.counts.CountsStore;
import org.neo4j.counts.CountsVisitor;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.io.pagecache.context.CursorContext;
import org.neo4j.token.TokenHolders;
import org.neo4j.token.api.TokenNotFoundException;

public abstract class AbstractInMemoryCountStore implements CountsStore {

    private final GraphStore graphStore;
    private final TokenHolders tokenHolders;

    public AbstractInMemoryCountStore(GraphStore graphStore, TokenHolders tokenHolders) {
        this.graphStore = graphStore;
        this.tokenHolders = tokenHolders;
    }

    @Override
    public Updater apply(long txId, CursorContext cursorContext) {
        throw new UnsupportedOperationException("Updates are not supported");
    }

    @Override
    public long nodeCount(int labelId, CursorContext cursorContext) {
        if (labelId == -1) {
            return graphStore.nodeCount();
        }

        String nodeLabel = null;
        try {
            nodeLabel = tokenHolders.labelTokens().getTokenById(labelId).name();
        } catch (TokenNotFoundException e) {
            throw new RuntimeException(e);
        }
        return -1; //TODO: implement graphStore.nodes().nodeCountForLabels(NodeLabel.listOf(nodeLabel));
    }

    @Override
    public long relationshipCount(
        int startLabelId, int typeId, int endLabelId, CursorContext cursorContext
    ) {
        // TODO: this is quite wrong
        return graphStore.relationshipCount();
    }

    @Override
    public void accept(CountsVisitor visitor, CursorContext cursorContext) {
        tokenHolders.labelTokens().getAllTokens().forEach(labelToken -> {
            visitor.visitNodeCount(labelToken.id(), nodeCount(labelToken.id(), cursorContext));
        });

        visitor.visitRelationshipCount(-1, -1, -1, graphStore.relationshipCount());
    }

    @Override
    public boolean consistencyCheck(ReporterFactory reporterFactory, CursorContext cursorContext) {
        return true;
    }

    @Override
    public void close() {

    }
}
