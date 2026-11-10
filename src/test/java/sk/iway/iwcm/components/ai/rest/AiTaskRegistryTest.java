package sk.iway.iwcm.components.ai.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.rest.AiChatStore.Scope;

/** Verifies that identical chat IDs remain independent across owners and domains. */
class AiTaskRegistryTest {

    /** Registers both scopes together and cancels only the selected scope without worker threads. */
    @ParameterizedTest
    @CsvSource({ "43,7,example.test", "42,8,example.test", "42,7,other.test" })
    void isolatesRegistrationAndCancellation(int userId, int domainId, String domainName) throws Exception {
        ExecutorService executor = mock(ExecutorService.class);
        AiTaskRegistry registry = new AiTaskRegistry(executor);
        Scope owner = new Scope(42, 7, "example.test", Path.of("."));
        Scope other = new Scope(userId, domainId, domainName, Path.of("."));
        String id = "934be8ec-6b59-4cbe-9575-a1a69f943dea";
        InputDataDTO ownerInput = new InputDataDTO(), otherInput = new InputDataDTO();
        AssistantResponseDTO answer = new AssistantResponseDTO();
        doAnswer(call -> {
            assertEquals("CANCELLED", registry.runChatTask(id, other, () -> answer, otherInput).getErrorCategory());
            assertTrue(otherInput.getCancellationToken().isCancelled());
            assertFalse(ownerInput.getCancellationToken().isCancelled());
            call.<Runnable>getArgument(0).run();
            return null;
        }).doAnswer(call -> {
            registry.cancelChatTask(id, other);
            call.<Runnable>getArgument(0).run();
            return null;
        }).when(executor).execute(any(Runnable.class));

        try {
            assertSame(answer, registry.runChatTask(id, owner, () -> answer, ownerInput));
        } finally {
            registry.destroy();
        }
    }
}
