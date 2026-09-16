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

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.webjetcms.ai.AiProvider;
import com.webjetcms.ai.AiProviderConfig;
import com.webjetcms.ai.ModelInfo;

import jakarta.servlet.ServletContext;
import sk.iway.iwcm.Constants;

class ConfiguredLocalProviderTest {

    @TempDir
    Path tempDirectory;

    /** Verifies lazy loading from absolute and server-relative paths and closing the delegate only once. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void opensConfiguredBundleLazilyAndClosesDelegateOnlyOnce(boolean useWebInfPath) throws Exception {
        String constantName = "ai_testLocalModelBundlePath";
        boolean constantExisted = Constants.containsKey(constantName);
        String originalValue = constantExisted ? Constants.getString(constantName) : null;
        Path bundle = tempDirectory.toRealPath().resolve("WEB-INF/local-ai-models/model.zip");
        ServletContext originalServletContext = Constants.getServletContext();
        ServletContext servletContext = mock(ServletContext.class);
        when(servletContext.getRealPath("/")).thenReturn(tempDirectory.toString());
        AiProvider delegate = mock(AiProvider.class);
        List<ModelInfo> models = List.of(mock(ModelInfo.class));
        AtomicInteger openCount = new AtomicInteger();

        try {
            Constants.setServletContext(servletContext);
            Constants.setString(constantName, useWebInfPath ? "/WEB-INF/local-ai-models/model.zip" : bundle.toString());
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
            Constants.setServletContext(originalServletContext);
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
