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
package positive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.processing.Generated;
import org.neo4j.gds.core.CypherMapWrapper;

@Generated("org.neo4j.gds.proc.ConfigurationProcessor")
public final class GSValidationConfig implements GSValidation {
    public GraphStoreValidationConfig() {

    }

    @Override
    public void graphStoreValidation(
        List<String> graphStore,
        Collection<String> selectedLabels,
        Collection<String> selectedRelationshipTypes
    ) {
        ArrayList<IllegalArgumentException> errors_ = new ArrayList<>();
        try {
            classSpecificName(graphStore, selectedLabels, selectedRelationshipTypes);
        } catch (IllegalArgumentException e) {
            errors_.add(e);
        }
        try {
            anotherCheck(graphStore, selectedLabels, selectedRelationshipTypes);
        } catch (IllegalArgumentException e) {
            errors_.add(e);
        }
        if (!errors_.isEmpty()) {
            if (errors_.size() == 1) {
                throw errors_.get(0);
            } else {
                String combinedErrorMsg = errors_
                    .stream()
                    .map(IllegalArgumentException::getMessage)
                    .collect(Collectors.joining(System.lineSeparator() + "\t\t\t\t",
                        "Multiple errors in configuration arguments:" + System.lineSeparator() + "\t\t\t\t",
                        ""
                    ));
                IllegalArgumentException combinedError = new IllegalArgumentException(combinedErrorMsg);
                errors_.forEach(error -> combinedError.addSuppressed(error));
                throw combinedError;
            }
        }
    }

    public static GSValidationConfig.Builder builder() {
        return new GSValidationConfig.Builder();
    }

    public static final class Builder {
        private final Map<String, Object> config;

        public Builder() {
            this.config = new HashMap<>();
        }

        public GSValidation build() {
            CypherMapWrapper config = CypherMapWrapper.create(this.config);
            return new GSValidationConfig();
        }
    }
}
