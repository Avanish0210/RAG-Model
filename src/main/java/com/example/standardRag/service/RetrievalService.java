package com.example.standardRag.service;

import com.example.standardRag.repository.DocumentRepository;
import com.example.standardRag.security.AuthUtil;
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
    private final AuthUtil authUtil;

    public List<Document> retrieve(String documentId, String query) {
        String userId = authUtil.getCurrentUserId().toString();
        documentRepository.findByDocumentIdAndUserIdAndActiveTrue(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        return hybridSearchService.search(documentId, userId, query, DEFAULT_TOP_K);
    }
}
