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
package org.neo4j.gds.core.io.schema;

import org.neo4j.gds.Orientation;
import org.neo4j.gds.RelationshipType;
import org.neo4j.gds.core.Aggregation;

public abstract class RelationshipSchemaVisitor extends InputRelationshipSchemaVisitor.Adapter {

    private RelationshipType relationshipType;
    private Aggregation aggregation;

    private Orientation orientation;

    public RelationshipType relationshipType() {
        return relationshipType;
    }

    public Orientation orientation() {
        return orientation;
    }

    @Override
    public Aggregation aggregation() {
        return aggregation;
    }

    @Override
    public boolean relationshipType(RelationshipType relationshipType) {
        this.relationshipType = relationshipType;
        return true;
    }

    @Override
    public boolean aggregation(Aggregation aggregation) {
        this.aggregation = aggregation;
        return true;
    }

    @Override
    public boolean orientation(Orientation orientation) {
        this.orientation = orientation;
        return true;
    }

    @Override
    protected void reset() {
        super.reset();
        relationshipType(null);
        aggregation(null);
    }
}
