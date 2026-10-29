package sk.iway.iwcm.components.ai.rest;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import com.webjetcms.ai.AiCancellationToken;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.providers.ProviderCallException;
import sk.iway.iwcm.users.UsersDB;

@Component
public class AiTaskRegistry implements DisposableBean {

    private static final String PREFIX = "ai_task_";
    private static final int MAX_CONCURRENT_TASKS = 32;
    private final ConcurrentMap<String, Future<AssistantResponseDTO>> futuresMap = new ConcurrentHashMap<>();
    private final ExecutorService executor;

    public AiTaskRegistry() {
        this(new ThreadPoolExecutor(
            MAX_CONCURRENT_TASKS,
            MAX_CONCURRENT_TASKS,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            task -> {
                Thread thread = new Thread(task, "webjet-ai-task");
                thread.setDaemon(true);
                return thread;
            }
        ));
    }

    AiTaskRegistry(ExecutorService executor) {
        this.executor = executor;
    }

    // store a future with ID
    public final void put(Long assistantId, Long timestamp, Future<AssistantResponseDTO> future, HttpServletRequest request) {
        String taskId = getTaskId(assistantId, timestamp, request);
        futuresMap.put(taskId, future);
    }

    // retrieve a future
    public final Future<AssistantResponseDTO> get(Long assistantId, Long timestamp, HttpServletRequest request) {
        return futuresMap.get( getTaskId(assistantId, timestamp, request) );
    }

    // cancel and remove a future
    public final boolean cancel(Long assistantId, Long timestamp, HttpServletRequest request) {
        Future<AssistantResponseDTO> future = futuresMap.remove( getTaskId(assistantId, timestamp, request) );
        if (future != null) {
            return future.cancel(true);
        }
        return false;
    }

    // remove without cancel - for example, when task is done
    public final void remove(Long assistantId, Long timestamp, HttpServletRequest request) {
        futuresMap.remove( getTaskId(assistantId, timestamp, request) );
    }

    /**
     * Registers a cancellable task before dispatch and propagates the caller's domain context.
     *
     * @param task assistant operation, including any sequential provider calls
     * @param inputData input receiving a fresh server-owned cancellation token
     * @param request authenticated request used to identify the task owner
     * @return completed response, or a structured cancellation result when the task is cancelled
     * @throws ProviderCallException if the assistant operation fails
     */
    public final AssistantResponseDTO runAssistantTask(Callable<AssistantResponseDTO> task, InputDataDTO inputData, HttpServletRequest request) throws ProviderCallException {
        String taskId = getTaskId(inputData.getAssistantId(), inputData.getTimestamp(), request);
        AiCancellationToken cancellation = new AiCancellationToken();
        inputData.setCancellationToken(cancellation);
        // Capture current RequestBean (thread-local) from caller thread
        RequestBean captured = SetCharacterEncodingFilter.getCurrentRequestBean();

        // Wrap original task to propagate RequestBean into executor thread
        Callable<AssistantResponseDTO> contextTask = () -> {
            RequestBean previous = SetCharacterEncodingFilter.getCurrentRequestBean();
            try {
                if (cancellation.isCancelled()) throw new CancellationException();
                if (captured != null) {
                    SetCharacterEncodingFilter.setCurrentRequestBean(captured);
                }
                AssistantResponseDTO result = task.call();
                if (cancellation.isCancelled()) throw new CancellationException();
                return result;
            } finally {
                // Restore previous (avoid leaking context into pooled thread)
                if (previous != null || captured != null) {
                    SetCharacterEncodingFilter.setCurrentRequestBean(previous);
                }
            }
        };

        FutureTask<AssistantResponseDTO> future = new CancellableTask(contextTask, cancellation);
        if (futuresMap.putIfAbsent(taskId, future) != null) {
            throw new IllegalStateException("An AI task with this identifier is already running");
        }

        try {
            executor.execute(future);
            // Return resposne
            return future.get();
        } catch (CancellationException e) {
            return AssistantResponseDTO.fromError(e);
        } catch (ExecutionException e) {
            // unwrap cause
            Throwable cause = e.getCause();
            if (cause instanceof CancellationException) return AssistantResponseDTO.fromError(cause);
            if (cause instanceof ProviderCallException providerException) throw providerException;
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            } else {
                throw new ProviderCallException(cause);
            }
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            return AssistantResponseDTO.fromError(new CancellationException("AI request cancelled"));
        } catch (RuntimeException e) {
            future.cancel(true);
            throw e;
        } finally {
            futuresMap.remove(taskId, future);
        }
    }

    /** Couples cancellation through the registry or its exposed Future to the provider request. */
    private static final class CancellableTask extends FutureTask<AssistantResponseDTO> {
        private final AiCancellationToken cancellation;

        private CancellableTask(Callable<AssistantResponseDTO> task, AiCancellationToken cancellation) {
            super(task);
            this.cancellation = cancellation;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancellation.cancel();
            return super.cancel(mayInterruptIfRunning);
        }
    }

    @Override
    public void destroy() {
        executor.shutdown();
        futuresMap.values().forEach(future -> future.cancel(true));
        futuresMap.clear();
        executor.shutdownNow();
    }

    private final String getTaskId(Long assistantId, Long timestamp, HttpServletRequest request) throws IllegalStateException {
        if(assistantId == null || assistantId < 1L) throw new IllegalStateException("Invalid param assistantId");
        if(timestamp == null || timestamp < 1L) throw new IllegalStateException("Invalid param timestamp");

        Identity currentUser = UsersDB.getCurrentUser(request);
        int userId = 0;
        if(currentUser != null) {
            userId = currentUser.getUserId();
        }

        return PREFIX + userId + "_" + assistantId + "_" + timestamp;
    }
}
