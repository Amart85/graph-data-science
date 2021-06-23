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
package org.neo4j.graphalgo.core.utils.progress.v2.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class IterativeTask extends Task {

    enum Mode {
        DYNAMIC,
        OPEN,
        FIXED
    }

    private final Supplier<List<Task>> subTasksSupplier;
    private final int iterations;
    private final Mode mode;


    public static IterativeTask dynamic(
        String description,
        Supplier<List<Task>> subTasksSupplier,
        int iterations
    ) {
        List<Task> unwindedTasks = new ArrayList<>();
        IntStream.range(0, iterations).forEach(i -> unwindedTasks.addAll(subTasksSupplier.get()));
        return new IterativeTask(
            description,
            unwindedTasks,
            subTasksSupplier,
            iterations,
            Mode.DYNAMIC
        );
    }

    public static IterativeTask open(
        String description,
        Supplier<List<Task>> subTasksSupplier
    ) {
        return new IterativeTask(
            description,
            new ArrayList<>(),
            subTasksSupplier,
            -1,
            Mode.OPEN
        );
    }

    private IterativeTask(
        String description,
        List<Task> subTasks,
        Supplier<List<Task>> subTasksSupplier,
        int iterations,
        Mode mode
    ) {
        super(description, subTasks);
        this.subTasksSupplier = subTasksSupplier;
        this.iterations = iterations;
        this.mode = mode;
    }

    @Override
    public Progress getProgress() {
        var progress = super.getProgress();

        if (mode == Mode.OPEN && status() != Status.FINISHED) {
            return ImmutableProgress.of(progress.progress(), -1);
        }

        return progress;
    }

    @Override
    public Task nextSubtask() {
        var maybeNextSubtask = subTasks().stream().filter(t -> t.status() == Status.OPEN).findFirst();

        if (maybeNextSubtask.isPresent()) {
            return maybeNextSubtask.get();
        } else if (mode == Mode.OPEN) {
            var newIterationTasks = subTasksSupplier.get();
            subTasks().addAll(newIterationTasks);
            return newIterationTasks.get(0);
        } else {
            throw new IllegalStateException("There are no more valid operations, this should never happen");
        }
    }

    @Override
    public void finish() {
        super.finish();
        subTasks().forEach(t -> {
            if (t.status() == Status.OPEN) {
                t.cancel();
            }
        });
    }

    public int currentIteration() {
        return (int) subTasks().stream().filter(t -> t.status() == Status.FINISHED).count() / subTasksSupplier
            .get()
            .size();
    }

}
