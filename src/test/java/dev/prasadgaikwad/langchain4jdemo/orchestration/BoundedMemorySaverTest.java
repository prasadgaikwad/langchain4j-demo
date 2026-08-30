package dev.prasadgaikwad.langchain4jdemo.orchestration;

import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.checkpoint.Checkpoint;
import org.junit.jupiter.api.Test;

import static java.util.Map.of;
import static org.assertj.core.api.Assertions.assertThat;

class BoundedMemorySaverTest {

    private Checkpoint checkpoint(String id) {
        return Checkpoint.builder()
                .id(id)
                .state(of("k", "v"))
                .nodeId("n" + id)
                .nextNodeId("next")
                .build();
    }

    private RunnableConfig config(String threadId) {
        return RunnableConfig.builder().threadId(threadId).build();
    }

    @Test
    void evictsOldestThreadsWhenTotalCheckpointCapIsExceeded() throws Exception {
        int cap = 4;
        BoundedMemorySaver saver = new BoundedMemorySaver(cap);

        // Two checkpoints in thread-a, then three in thread-b -> 5 total > cap of 4.
        // The first-inserted thread (a) must be fully evicted.
        Checkpoint a1 = checkpoint("a1");
        Checkpoint a2 = checkpoint("a2");
        String threadA = "session-a:run-1";
        saver.put(config(threadA), a1);
        saver.put(config(threadA), a2);

        String threadB = "session-b:run-1";
        Checkpoint b1 = checkpoint("b1");
        Checkpoint b2 = checkpoint("b2");
        Checkpoint b3 = checkpoint("b3");
        saver.put(config(threadB), b1);
        saver.put(config(threadB), b2);
        saver.put(config(threadB), b3);

        // The oldest thread (a) is evicted; only thread-b checkpoints remain.
        assertThat(saver.list(config(threadA))).isEmpty();
        assertThat(saver.list(config(threadB))).hasSize(3);
        assertThat(totalAcross(saver, threadA, threadB)).isLessThanOrEqualTo(cap);
    }

    @Test
    void keepsEverythingUnderTheCap() throws Exception {
        int cap = 100;
        BoundedMemorySaver saver = new BoundedMemorySaver(cap);
        saver.put(config("t1"), checkpoint("x1"));
        saver.put(config("t1"), checkpoint("x2"));
        saver.put(config("t2"), checkpoint("y1"));

        assertThat(saver.list(config("t1"))).hasSize(2);
        assertThat(saver.list(config("t2"))).hasSize(1);
    }

    private int totalAcross(BoundedMemorySaver saver, String... threads) {
        int total = 0;
        for (String t : threads) {
            total += saver.list(config(t)).size();
        }
        return total;
    }
}
