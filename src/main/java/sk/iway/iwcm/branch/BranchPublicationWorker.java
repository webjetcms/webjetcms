package sk.iway.iwcm.branch;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

/** Wakes the publication queue independently of the initiating HTTP request. */
@Component
class BranchPublicationWorker {
    // One pending scan covers all committed requests. Cron picks up requests left on shutdown.
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1), task -> new Thread(task, "branch-publication"),
            new ThreadPoolExecutor.DiscardPolicy());

    void wakeUp() {
        executor.execute(BranchScheduler::publishRequested);
    }

    @PreDestroy
    void stop() {
        executor.shutdownNow();
    }
}
