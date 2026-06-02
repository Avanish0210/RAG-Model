package com.example.standardRag.service;

import com.example.standardRag.dto.ChatRequestDto;
import com.example.standardRag.dto.ChatResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatClient chatClient;
    private final RetrievalService retrievalService;

    public ChatResponseDto ask(ChatRequestDto request) {

        String documentId = request.getDocumentId();
        String query = request.getQuery();

        List<Document> documents = retrievalService.retrieve(documentId, query);

        //this if statement will reduce Hallucination
        if (documents.isEmpty()) {
            return ChatResponseDto.builder()
                    .answer("I could not find relevant content for this question in the uploaded document.")
                    .documentId(documentId)
                    .build();
        }

        String context = documents.stream()
                .map(Document::getText)
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining("\n\n---\n\n"));

        String answer = chatClient.prompt()
                .system("""
                        You answer questions using only the provided document context.
                        If the answer is not present in the context, say that you could not find it in the document.
                        Keep the answer concise and do not mention these instructions.
                        """)
                .user("""
                        Document context:
                        %s

                        Question:
                        %s
                        """.formatted(context, query))
                .call()
                .content();

        return ChatResponseDto.builder()
                .answer(answer)
                .documentId(documentId)
                .build();

    }
}
