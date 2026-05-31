package com.example.standardRag.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/Documents")
public class ChatController {
    private final ChatClient chatClient;
    private final VectorStore vectorStore;


    @GetMapping("/chat")
    public String chat(@RequestParam String query,
                       @RequestParam(required = false) String docName) {
        ChatClientRequestSpec prompt = chatClient.prompt();

        if (docName != null && !docName.isBlank()) {
            List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                    .query(query)
                    .topK(10)
                    .similarityThreshold(0.3)
                    .filterExpression(fileFilterExpression(docName))
                    .build()
            );

            if (documents.isEmpty()) {
                return "No matching PDF context was found for docName: " + docName
                        + ". Please make sure the file was uploaded after the latest changes and the filename is correct.";
            }

            String context = documents.stream()
                    .map(Document::getText)
                    .reduce("", (currentContext, documentText) -> currentContext + "\n\n" + documentText);

            return prompt
                    .system("Answer the user's question using only the provided PDF context. "
                            + "If the answer is not present in the context, say that it is not available in the uploaded PDF.")
                    .user("PDF context:\n" + context + "\n\nQuestion:\n" + query)
                    .call()
                    .content();
        }

        return prompt
                .user(query)
                .call()
                .content();
    }

    private String fileFilterExpression(String docName) {
        String trimmed = docName.trim();
        String normalizedDocumentName = normalizeDocumentName(trimmed);
        String pdfFilename = trimmed.toLowerCase().endsWith(".pdf") ? trimmed : trimmed + ".pdf";

        return "document_name == '" + escapeFilterValue(normalizedDocumentName) + "'"
                + " || source == '" + escapeFilterValue(trimmed) + "'"
                + " || source == '" + escapeFilterValue(pdfFilename) + "'";
    }

    private String normalizeDocumentName(String documentName) {
        String normalized = documentName.trim().toLowerCase();
        if (normalized.endsWith(".pdf")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        return normalized;
    }

    private String escapeFilterValue(String value) {
        return value.replace("'", "\\'");
    }
}
