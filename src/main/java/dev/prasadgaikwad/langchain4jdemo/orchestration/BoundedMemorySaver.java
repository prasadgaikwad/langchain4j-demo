package dev.prasadgaikwad.langchain4jdemo.orchestration;

import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.checkpoint.AbstractCheckpointSaver;
import org.bsc.langgraph4j.checkpoint.Checkpoint;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Bounded in-memory checkpoint saver (issue #252).
 *
 * <p>The plain langgraph4j {@code MemorySaver} never evicts: every thread's
 * checkpoints accumulate for the lifetime of the JVM, leaking without bound on a
 * long-running server. This saver caps the <em>total</em> number of checkpoints
 * across all threads and evicts the oldest threads (by first-insertion order)
 * once the cap is exceeded. It is thread-safe via the {@link
 * AbstractCheckpointSaver} lock.</p>
 */
public final class BoundedMemorySaver extends AbstractCheckpointSaver {

    private static final float LOAD_FACTOR = 0.75f;

    private final int maxCheckpoints;
    /** Insertion-ordered so the oldest thread (evicted first) sits at the head. */
    private final Map<String, LinkedList<Checkpoint>> checkpointsByThread = new LinkedHashMap<>(16, LOAD_FACTOR, false);

    public BoundedMemorySaver(int maxCheckpoints) {
        this.maxCheckpoints = Math.max(1, maxCheckpoints);
    }

    public int maxCheckpoints() {
        return maxCheckpoints;
    }

    @Override
    protected LinkedList<Checkpoint> loadCheckpoints(RunnableConfig config) {
        final var threadId = threadId(config);
        return checkpointsByThread.computeIfAbsent(threadId, k -> new LinkedList<>());
    }

    @Override
    protected void insertedCheckpoint(RunnableConfig config, LinkedList<Checkpoint> checkpoints, Checkpoint checkpoint) {
        evictIfNeeded();
    }

    @Override
    protected void updatedCheckpoint(RunnableConfig config, LinkedList<Checkpoint> checkpoints, Checkpoint checkpoint) {
        evictIfNeeded();
    }

    @Override
    protected Tag releaseCheckpoints(RunnableConfig config, LinkedList<Checkpoint> checkpoints) {
        final var threadId = threadId(config);
        return new Tag(threadId, checkpointsByThread.remove(threadId));
    }

    private void evictIfNeeded() {
        while (totalCheckpoints() > maxCheckpoints) {
            Iterator<Map.Entry<String, LinkedList<Checkpoint>>> it = checkpointsByThread.entrySet().iterator();
            if (!it.hasNext()) {
                break;
            }
            it.next();
            it.remove();
        }
    }

    private int totalCheckpoints() {
        int total = 0;
        for (LinkedList<Checkpoint> list : checkpointsByThread.values()) {
            total += list.size();
        }
        return total;
    }
}
