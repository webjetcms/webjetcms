package sk.iway.iwcm.rag.vectorstore;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class MariaDbVectorStoreTest {

    @Test
    void serializesAndParsesMariaDbVectorText() {
        float[] embedding = {0.125f, -2.5f, 3.75f};

        String serialized = MariaDbVectorStore.vectorToString(embedding);

        assertEquals("[0.125,-2.5,3.75]", serialized);
        assertArrayEquals(embedding, MariaDbVectorStore.parseVector(serialized));
        assertArrayEquals(new float[0], MariaDbVectorStore.parseVector("[]"));
    }

    @Test
    void supportsOnlyMariaDbIndexMetrics() {
        assertTrue(MariaDbVectorStore.isSupportedMetric("cosine"));
        assertTrue(MariaDbVectorStore.isSupportedMetric("l2"));
        assertFalse(MariaDbVectorStore.isSupportedMetric("inner_product"));
        assertFalse(MariaDbVectorStore.isSupportedMetric("unknown"));
    }

    @Test
    void buildsVectorFirstCosineSearchWithPerStatementEfSearch() {
        String sql = MariaDbVectorStore.buildVectorSearchSql(
            "cosine",
            " AND c.domain_id = ?",
            100
        );

        assertTrue(sql.startsWith("SET STATEMENT mhnsw_ef_search=100 FOR SELECT"));
        assertTrue(sql.contains(
            "FROM rag_embedding_vectors v FORCE INDEX (idx_rag_embedding_vector)"));
        assertTrue(sql.contains("JOIN rag_embedding_chunks c ON c.id = v.chunk_id"));
        assertTrue(sql.contains("1 - ranked.distance AS similarity"));
        assertTrue(sql.contains(
            "ORDER BY VEC_DISTANCE_COSINE(v.embedding, VEC_FromText(?)) ASC"));
        assertTrue(sql.contains("AND c.domain_id = ?"));
    }

    @Test
    void buildsEuclideanSearchWithoutSessionOverride() {
        String sql = MariaDbVectorStore.buildVectorSearchSql("l2", "", null);

        assertFalse(sql.startsWith("SET STATEMENT"));
        assertTrue(sql.contains("1 / (1 + ranked.distance) AS similarity"));
        assertTrue(sql.contains(
            "ORDER BY VEC_DISTANCE_EUCLIDEAN(v.embedding, VEC_FromText(?)) ASC"));
    }

    @Test
    void recognizesConfiguredMetricInMariaDbVectorIndexDefinition() {
        String cosineDefinition = """
            CREATE TABLE `rag_embedding_vectors` (
              `embedding` vector(4) NOT NULL,
              VECTOR KEY `idx_rag_embedding_vector` (`embedding`) `M`='16' `DISTANCE`='cosine'
            )
            """;

        assertTrue(MariaDbVectorStore.matchesVectorIndexDefinition(cosineDefinition, "cosine"));
        assertFalse(MariaDbVectorStore.matchesVectorIndexDefinition(cosineDefinition, "l2"));
    }

    @Test
    void recognizesAllRequiredMariaDbChunkIndexes() {
        assertTrue(MariaDbVectorStore.matchesChunkIndexDefinitions(completeChunkTableDefinition()));
    }

    @Test
    void rejectsMissingFulltextAndOldUniqueIndexes() {
        String completeDefinition = completeChunkTableDefinition();

        assertFalse(MariaDbVectorStore.matchesChunkIndexDefinitions(
            completeDefinition.replace(
                "FULLTEXT KEY `idx_rag_chunk_text_fts` (`chunk_text`),\n",
                ""
            )
        ));
        assertFalse(MariaDbVectorStore.matchesChunkIndexDefinitions(
            completeDefinition.replace(
                "(`entity_type`,`entity_id`,`chunk_index`,`embedding_provider`,`embedding_model`)",
                "(`entity_type`,`entity_id`,`chunk_index`,`embedding_model`)"
            )
        ));
    }

    @Test
    void recognizesOnlyCascadingVectorForeignKey() {
        String vectorDefinition = """
            CREATE TABLE `rag_embedding_vectors` (
              `chunk_id` bigint(20) NOT NULL,
              `embedding` vector(4) NOT NULL,
              CONSTRAINT `legacy_name` FOREIGN KEY (`chunk_id`)
                REFERENCES `rag_embedding_chunks` (`id`) ON DELETE CASCADE
            )
            """;

        assertTrue(MariaDbVectorStore.matchesVectorForeignKeyDefinition(vectorDefinition));
        assertFalse(MariaDbVectorStore.matchesVectorForeignKeyDefinition(
            vectorDefinition.replace(" ON DELETE CASCADE", "")
        ));
    }

    @Test
    void preservesCompleteChunkIndexes() {
        String repairSql = MariaDbVectorStore.buildChunkIndexRepairSql(
            completeChunkTableDefinition(),
            requiredChunkIndexNames()
        );

        assertNull(repairSql);
    }

    @Test
    void replacesWrongUniqueIndexInOneAlterStatement() {
        String oldDefinition = completeChunkTableDefinition().replace(
            "(`entity_type`,`entity_id`,`chunk_index`,`embedding_provider`,`embedding_model`)",
            "(`entity_type`,`entity_id`,`chunk_index`,`embedding_model`)"
        );

        String repairSql = MariaDbVectorStore.buildChunkIndexRepairSql(
            oldDefinition,
            requiredChunkIndexNames()
        );

        assertEquals(1, repairSql.split("ALTER TABLE", -1).length - 1);
        assertTrue(repairSql.contains("DROP INDEX `uq_rag_chunk`, ADD UNIQUE INDEX `uq_rag_chunk`"));
        assertFalse(repairSql.contains("DROP INDEX `idx_rag_chunk_entity`"));
    }

    @Test
    void replacesLegacyVectorIndexAtomically() {
        String repairSql = MariaDbVectorStore.buildVectorIndexRepairSql(
            List.of("legacy_vector_name"),
            "cosine"
        );

        assertEquals(1, repairSql.split("ALTER TABLE", -1).length - 1);
        assertTrue(repairSql.contains("DROP INDEX `legacy_vector_name`"));
        assertTrue(repairSql.contains(
            "ADD VECTOR INDEX `idx_rag_embedding_vector` (embedding) M=16 DISTANCE=cosine"));
    }

    @Test
    void resetsDimensionsAndVectorIndexInOneAlterStatement() {
        String resetSql = MariaDbVectorStore.buildVectorDimensionResetSql(
            List.of("legacy_vector_name"),
            768,
            "l2",
            true
        );

        assertEquals(1, resetSql.split("ALTER TABLE", -1).length - 1);
        assertTrue(resetSql.contains("DROP INDEX `legacy_vector_name`"));
        assertTrue(resetSql.contains("MODIFY COLUMN embedding VECTOR(768) NOT NULL"));
        assertTrue(resetSql.contains("DISTANCE=euclidean"));
    }

    private String completeChunkTableDefinition() {
        return """
            CREATE TABLE `rag_embedding_chunks` (
              `id` bigint(20) NOT NULL AUTO_INCREMENT,
              `chunk_text` text NOT NULL,
              UNIQUE KEY `uq_rag_chunk` (`entity_type`,`entity_id`,`chunk_index`,`embedding_provider`,`embedding_model`),
              KEY `idx_rag_chunk_entity` (`entity_type`,`entity_id`),
              KEY `idx_rag_chunk_domain_lang` (`domain_id`,`language`,`status`),
              FULLTEXT KEY `idx_rag_chunk_text_fts` (`chunk_text`),
              PRIMARY KEY (`id`)
            ) ENGINE=InnoDB
            """;
    }

    private List<String> requiredChunkIndexNames() {
        return List.of(
            "PRIMARY",
            "uq_rag_chunk",
            "idx_rag_chunk_entity",
            "idx_rag_chunk_domain_lang",
            "idx_rag_chunk_text_fts"
        );
    }
}
