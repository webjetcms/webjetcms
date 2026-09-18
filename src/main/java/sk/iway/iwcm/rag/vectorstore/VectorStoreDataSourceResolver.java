package sk.iway.iwcm.rag.vectorstore;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Locale;

import javax.sql.DataSource;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;

/**
 * Resolves the datasource and database backend used by the RAG vector store.
 */
public final class VectorStoreDataSourceResolver {

    public static final String RAG_DATASOURCE_NAME = "rag_jpa";
    public static final String PRIMARY_DATASOURCE_NAME = "iwcm";

    private static final int MINIMUM_MARIADB_MAJOR_VERSION = 11;
    private static final int MINIMUM_MARIADB_MINOR_VERSION = 8;

    private static volatile CachedResolution cachedResolution;

    private VectorStoreDataSourceResolver() {
        // Utility class
    }

    /**
     * Resolves the effective RAG datasource. An explicitly configured {@code rag_jpa}
     * datasource is authoritative and is never replaced by the primary datasource when
     * it is unavailable or unsupported.
     *
     * @return datasource resolution including backend and diagnostic details
     */
    public static Resolution resolve() {
        DBPool dbPool = DBPool.getInstance();
        DataSource ragDataSource = dbPool.getDataSource(RAG_DATASOURCE_NAME);
        boolean ragDataSourceConfigured = DBPool.isDataSourceConfigured(RAG_DATASOURCE_NAME);
        DataSource primaryDataSource = ragDataSource == null && ragDataSourceConfigured == false
            ? dbPool.getDataSource(PRIMARY_DATASOURCE_NAME)
            : null;
        return resolve(ragDataSource, ragDataSourceConfigured, primaryDataSource);
    }

    static Resolution resolve(DataSource ragDataSource, DataSource primaryDataSource) {
        return resolve(ragDataSource, ragDataSource != null, primaryDataSource);
    }

    static Resolution resolve(
        DataSource ragDataSource,
        boolean ragDataSourceConfigured,
        DataSource primaryDataSource
    ) {
        if (ragDataSource != null) {
            return resolveSelectedDataSource(RAG_DATASOURCE_NAME, ragDataSource, true);
        }
        if (ragDataSourceConfigured) {
            return new Resolution(
                RAG_DATASOURCE_NAME,
                VectorStoreBackend.UNSUPPORTED,
                null,
                null,
                "Configured RAG datasource rag_jpa is unavailable",
                true
            );
        }
        if (primaryDataSource != null) {
            return resolveSelectedDataSource(PRIMARY_DATASOURCE_NAME, primaryDataSource, false);
        }
        return new Resolution(null, VectorStoreBackend.UNSUPPORTED, null, null,
            "Neither rag_jpa nor iwcm datasource is configured", false);
    }

    private static Resolution resolveSelectedDataSource(String dataSourceName, DataSource dataSource, boolean explicit) {
        CachedResolution cached = cachedResolution;
        if (cached != null && cached.dataSource() == dataSource) {
            return cached.resolution();
        }

        Resolution resolution = inspect(dataSourceName, dataSource, explicit);
        if (resolution.isSupported()) {
            cachedResolution = new CachedResolution(dataSource, resolution);
        }
        return resolution;
    }

    private static Resolution inspect(String dataSourceName, DataSource dataSource, boolean explicit) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            String productName = metadata.getDatabaseProductName();
            String productVersion = metadata.getDatabaseProductVersion();
            String normalizedProductName = normalize(productName);

            if (normalizedProductName.contains("postgresql")) {
                return new Resolution(dataSourceName, VectorStoreBackend.POSTGRESQL, productName, productVersion,
                    null, explicit);
            }

            if (normalizedProductName.contains("mariadb")) {
                int majorVersion = metadata.getDatabaseMajorVersion();
                int minorVersion = metadata.getDatabaseMinorVersion();
                if (isSupportedMariaDbVersion(majorVersion, minorVersion)) {
                    return new Resolution(dataSourceName, VectorStoreBackend.MARIADB, productName, productVersion,
                        null, explicit);
                }
                return new Resolution(dataSourceName, VectorStoreBackend.UNSUPPORTED, productName, productVersion,
                    "MariaDB 11.8 or newer is required, detected " + formatVersion(productVersion, majorVersion, minorVersion), explicit);
            }

            return new Resolution(dataSourceName, VectorStoreBackend.UNSUPPORTED, productName, productVersion,
                "Unsupported RAG database product: " + safeValue(productName), explicit);
        } catch (Exception e) {
            return new Resolution(dataSourceName, VectorStoreBackend.UNSUPPORTED, null, null,
                "Unable to inspect RAG datasource " + dataSourceName + ": " + safeValue(e.getMessage()), explicit);
        }
    }

    private static boolean isSupportedMariaDbVersion(int majorVersion, int minorVersion) {
        return majorVersion > MINIMUM_MARIADB_MAJOR_VERSION ||
            (majorVersion == MINIMUM_MARIADB_MAJOR_VERSION && minorVersion >= MINIMUM_MARIADB_MINOR_VERSION);
    }

    private static String formatVersion(String productVersion, int majorVersion, int minorVersion) {
        if (productVersion != null && productVersion.isBlank() == false) return productVersion;
        return majorVersion + "." + minorVersion;
    }

    private static String safeValue(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the name of a supported RAG datasource.
     *
     * @return datasource name or {@code null} when its backend is unsupported
     */
    public static String getRagDataSourceName() {
        Resolution resolution = resolve();
        return resolution.isSupported() ? resolution.dataSourceName() : null;
    }

    /**
     * Checks whether semantic search is enabled and a supported vector backend is available.
     *
     * @return {@code true} when the RAG vector store may be used
     */
    public static boolean isRagAvailable() {
        return Constants.getBoolean("ragSemanticSearchEnabled") && resolve().isSupported();
    }

    /**
     * Result of selecting and inspecting the effective RAG datasource.
     *
     * @param dataSourceName selected datasource name
     * @param backend detected vector backend
     * @param databaseProductName JDBC database product name
     * @param databaseProductVersion JDBC database product version
     * @param reason diagnostic reason when the backend is unsupported
     * @param explicit whether the selected datasource is the explicit {@code rag_jpa} datasource
     */
    public record Resolution(
        String dataSourceName,
        VectorStoreBackend backend,
        String databaseProductName,
        String databaseProductVersion,
        String reason,
        boolean explicit
    ) {
        public boolean isSupported() {
            return backend != VectorStoreBackend.UNSUPPORTED;
        }
    }

    private record CachedResolution(DataSource dataSource, Resolution resolution) {
    }
}
