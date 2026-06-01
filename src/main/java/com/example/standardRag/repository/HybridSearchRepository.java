package com.example.standardRag.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.document.Document;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Repository
public class HybridSearchRepository {
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final RowMapper<Document> documentRowMapper;

    public HybridSearchRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
        this.documentRowMapper = (rs, rowNum) -> Document.builder()
                .id(rs.getString("id"))
                .text(rs.getString("content"))
                .metadata(readMetadata(rs.getString("metadata")))
                .score(rs.getDouble("score"))
                .build();
    }

    public void refreshSearchableText(String documentId) {
        ensureSearchableTextColumn();
        jdbcTemplate.update("""
                        UPDATE vector_store
                        SET searchable_text = to_tsvector(
                            'english',
                            lower(regexp_replace(coalesce(content, ''), '\\s+', ' ', 'g'))
                        )
                        WHERE metadata ->> 'documentId' = ?
                        """,
                documentId
        );
    }

    public void ensureSearchableTextColumn() {
        jdbcTemplate.execute("ALTER TABLE vector_store ADD COLUMN IF NOT EXISTS searchable_text tsvector");
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF EXISTS (
                        SELECT 1
                        FROM information_schema.columns
                        WHERE table_schema = current_schema()
                          AND table_name = 'vector_store'
                          AND column_name = 'searchable_text'
                          AND data_type <> 'tsvector'
                    ) THEN
                        ALTER TABLE vector_store
                        ALTER COLUMN searchable_text TYPE tsvector
                        USING to_tsvector('english', coalesce(searchable_text::text, content, ''));
                    END IF;
                END
                $$
                """);
        jdbcTemplate.execute("""
                UPDATE vector_store
                SET searchable_text = to_tsvector(
                    'english',
                    lower(regexp_replace(coalesce(content, ''), '\\s+', ' ', 'g'))
                )
                WHERE searchable_text IS NULL
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS vector_store_searchable_text_idx
                ON vector_store
                USING GIN (searchable_text)
                """);
    }

    public List<Document> keywordSearch(String documentId, String query, int limit) {
        ensureSearchableTextColumn();
        String normalizedQuery = normalizeQuery(query);
        if (normalizedQuery.isBlank()) {
            return Collections.emptyList();
        }

        return jdbcTemplate.query("""
                        SELECT id::text,
                               content,
                               metadata::text,
                               ts_rank_cd(
                                   searchable_text,
                                   plainto_tsquery('english', ?)
                               ) AS score
                        FROM vector_store
                        WHERE metadata ->> 'documentId' = ?
                          AND searchable_text @@ plainto_tsquery('english', ?)
                        ORDER BY score DESC
                        LIMIT ?
                        """,
                documentRowMapper,
                normalizedQuery,
                documentId,
                normalizedQuery,
                limit
        );
    }

    private Map<String, Object> readMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return Collections.emptyMap();
        }

        try {
            return objectMapper.readValue(metadata, new TypeReference<>() {
            });
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    private String normalizeQuery(String query) {
        if (query == null) {
            return "";
        }

        return WHITESPACE.matcher(query.trim()).replaceAll(" ");
    }

}
