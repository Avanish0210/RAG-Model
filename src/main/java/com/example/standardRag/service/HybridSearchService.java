package com.example.standardRag.service;

import com.example.standardRag.repository.HybridSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HybridSearchService {
    private static final int RRF_K = 60;

    private final VectorStore vectorStore;
    private final HybridSearchRepository hybridSearchRepository;

    public List<Document> search(String documentId, String userId, String query, int topK) {
        int candidateLimit = Math.max(topK * 3, topK);

        List<Document> vectorResults = vectorSearch(documentId, userId, query, candidateLimit);
        List<Document> keywordResults = hybridSearchRepository.keywordSearch(documentId, userId, query, candidateLimit);

        return reciprocalRankFusion(vectorResults, keywordResults, topK);
    }

    private List<Document> vectorSearch(String documentId, String userId, String query, int limit) {
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(limit)
                        .similarityThresholdAll()
                        .filterExpression("documentId == '" + escapeFilterValue(documentId) + "' && userId == '" + escapeFilterValue(userId) + "' && active == true")
                        .build()
        );
    }

    private List<Document> reciprocalRankFusion(List<Document> vectorResults, List<Document> keywordResults, int topK) {
        Map<String, RankedDocument> rankedDocuments = new LinkedHashMap<>();
        addRankedResults(rankedDocuments, vectorResults, "vectorScore");
        addRankedResults(rankedDocuments, keywordResults, "keywordScore");

        return rankedDocuments.values().stream()
                .sorted(Comparator.comparingDouble(RankedDocument::score).reversed())
                .limit(topK)
                .map(RankedDocument::toDocument)
                .toList();
    }

    private void addRankedResults(Map<String, RankedDocument> rankedDocuments, List<Document> documents, String scoreName) {
        for (int index = 0; index < documents.size(); index++) {
            Document document = documents.get(index);
            String key = document.getId();
            double rrfScore = 1.0 / (RRF_K + index + 1);

            RankedDocument rankedDocument = rankedDocuments.computeIfAbsent(key, ignored -> new RankedDocument(document));
            rankedDocument.addScore(rrfScore, scoreName, document.getScore());
        }
    }

    private String escapeFilterValue(String value) {
        return value.replace("'", "\\'");
    }

    private static final class RankedDocument {
        private final Document document;
        private final Map<String, Object> metadata;
        private double score;

        private RankedDocument(Document document) {
            this.document = document;
            this.metadata = new LinkedHashMap<>(document.getMetadata());
        }

        private void addScore(double rrfScore, String scoreName, Double sourceScore) {
            score += rrfScore;
            if (sourceScore != null) {
                metadata.put(scoreName, sourceScore);
            }
            metadata.put("rrfScore", score);
        }

        private double score() {
            return score;
        }

        private Document toDocument() {
            return Document.builder()
                    .id(document.getId())
                    .text(document.getText())
                    .metadata(metadata)
                    .score(score)
                    .build();
        }
    }
}
