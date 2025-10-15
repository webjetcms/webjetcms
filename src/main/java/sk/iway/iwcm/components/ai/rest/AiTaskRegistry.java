package sk.iway.iwcm.components.ai.rest;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.providers.ProviderCallException;
import sk.iway.iwcm.users.UsersDB;

@Component
public class AiTaskRegistry {

    private static final String PREFIX = "ai_task_";
    private final ConcurrentMap<String, Future<AssistantResponseDTO>> futuresMap = new ConcurrentHashMap<>();

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

    public final AssistantResponseDTO runAssistantTask(Callable<AssistantResponseDTO> task, InputDataDTO inputData, HttpServletRequest request) throws ProviderCallException {
        // Capture current RequestBean (thread-local) from caller thread
        RequestBean captured = SetCharacterEncodingFilter.getCurrentRequestBean();

        // Wrap original task to propagate RequestBean into executor thread
        Callable<AssistantResponseDTO> contextTask = () -> {
            RequestBean previous = SetCharacterEncodingFilter.getCurrentRequestBean();
            try {
                if (captured != null) {
                    SetCharacterEncodingFilter.setCurrentRequestBean(captured);
                }
                return task.call();
            } finally {
                // Restore previous (avoid leaking context into pooled thread)
                if (previous != null || captured != null) {
                    SetCharacterEncodingFilter.setCurrentRequestBean(previous);
                }
            }
        };

        ExecutorService executor = Executors.newSingleThreadExecutor();

        // submit task
        Future<AssistantResponseDTO> future = executor.submit(contextTask);

        //store it
        put(inputData.getAssistantId(), inputData.getTimestamp(), future, request);

        try {
            // Return resposne
            return future.get();
        } catch (CancellationException e) {
            // Task was cancelled - Its OK
            return null;
        } catch (ExecutionException e) {
            //Remove it from map
            remove(inputData.getAssistantId(), inputData.getTimestamp(), request);

            // unwrap cause
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            } else {
                throw new ProviderCallException(cause);
            }
        } catch (InterruptedException e) {
            //Remove it from map
            remove(inputData.getAssistantId(), inputData.getTimestamp(), request);

            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted", e);
        }
    }

    private final String getTaskId(Long assistantId, Long timestamp, HttpServletRequest request) throws IllegalStateException {
        if(assistantId == null || assistantId < 1L) throw new IllegalStateException("Invalid param assistantId");
        if(timestamp == null || timestamp < 1L) throw new IllegalStateException("Invalid param timestamp");

        Identity currentUser = UsersDB.getCurrentUser(request);

        return PREFIX + currentUser.getUserId() + "_" + assistantId + "_" + timestamp;
    }
}
