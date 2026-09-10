package sk.iway.iwcm.components.ai.providers.local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.webjetcms.ai.AiProvider;
import com.webjetcms.ai.AiProviderConfig;
import com.webjetcms.ai.ModelInfo;

import sk.iway.iwcm.Constants;

class ConfiguredLocalProviderTest {

    @TempDir
    Path tempDirectory;

    @Test
    void opensConfiguredBundleLazilyAndClosesDelegateOnlyOnce() throws Exception {
        String constantName = "ai_testLocalModelBundlePath";
        boolean constantExisted = Constants.containsKey(constantName);
        String originalValue = constantExisted ? Constants.getString(constantName) : null;
        Path bundle = tempDirectory.resolve("model.zip");
        AiProvider delegate = mock(AiProvider.class);
        List<ModelInfo> models = List.of(mock(ModelInfo.class));
        AtomicInteger openCount = new AtomicInteger();

        try {
            Constants.setString(constantName, bundle.toString());
            when(delegate.listModels(any(AiProviderConfig.class))).thenReturn(models);
            ConfiguredLocalProvider provider = new TestConfiguredLocalProvider(
                constantName,
                configuredBundle -> {
                    assertEquals(bundle, configuredBundle);
                    openCount.incrementAndGet();
                    return delegate;
                }
            );

            assertTrue(provider.isConfigured());
            assertSame(models, provider.listModels(AiProviderConfig.empty()));
            assertSame(models, provider.listModels(AiProviderConfig.empty()));
            assertEquals(1, openCount.get());

            provider.close();
            provider.close();
            verify(delegate, times(1)).close();
        } finally {
            if (constantExisted) Constants.setString(constantName, originalValue);
            else Constants.deleteConstant(constantName);
        }
    }

    private static final class TestConfiguredLocalProvider extends ConfiguredLocalProvider {

        private TestConfiguredLocalProvider(String bundlePathConstant, ProviderOpener opener) {
            super("test-local-provider", bundlePathConstant, opener);
        }
    }
}
