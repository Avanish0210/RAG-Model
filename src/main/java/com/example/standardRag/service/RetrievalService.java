package com.example.standardRag.service;

import com.example.standardRag.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RetrievalService {
    private static final int DEFAULT_TOP_K = 8;

    private final DocumentRepository documentRepository;
    private final HybridSearchService hybridSearchService;

    public List<Document> retrieve(String documentId, String query) {
        documentRepository.findByDocumentIdAndActiveTrue(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        return hybridSearchService.search(documentId, query, DEFAULT_TOP_K);
    }
}
