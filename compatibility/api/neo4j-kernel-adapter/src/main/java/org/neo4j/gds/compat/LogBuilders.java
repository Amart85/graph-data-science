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

import org.immutables.builder.Builder;
import org.neo4j.logging.Level;
import org.neo4j.logging.Log;
import org.neo4j.logging.LogTimeZone;
import org.neo4j.logging.log4j.Log4jLogProvider;
import org.neo4j.logging.log4j.LogConfig;

import java.io.OutputStream;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;

final class LogBuilders {

    @Builder.Factory
    static Log outputStreamLog(
        @Builder.Parameter OutputStream outputStream,
        Optional<Level> level,
        Optional<ZoneId> zoneId,
        Optional<String> category
    ) {
        var logTimeZone = Arrays
            .stream(LogTimeZone.values())
            .filter(tz -> tz.getZoneId().equals(zoneId.orElse(ZoneOffset.UTC)))
            .findAny()
            .orElseThrow(() -> new IllegalArgumentException("Can only log in UTC or " + LogTimeZone.SYSTEM.getZoneId()));
        var context = LogConfig
            .createBuilder(outputStream, level.orElse(Level.INFO))
            .withCategory(category.isPresent())
            .withTimezone(logTimeZone)
            .build();

        return new Log4jLogProvider(context).getLog(category.orElse(""));
    }

    private LogBuilders() {
        throw new UnsupportedOperationException("No instances");
    }
}
