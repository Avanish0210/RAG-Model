package com.example.standardRag.service;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.example.standardRag.dto.UploadResponseDto;
import com.example.standardRag.entity.DocumentEntity;
import com.example.standardRag.repository.DocumentRepository;
import com.example.standardRag.repository.HybridSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class IngestionService {
    private final DocumentRepository documentRepository;
    private final HybridSearchRepository hybridSearchRepository;
    private final VectorStore vectorStore;

    @Transactional
    public UploadResponseDto ingest(MultipartFile file) throws IOException {
        String source = file.getOriginalFilename();

        String normalizedName = normalizeDocumentName(source);

        String documentId = "DOC-" + NanoIdUtils.randomNanoId(
                NanoIdUtils.DEFAULT_NUMBER_GENERATOR,
                NanoIdUtils.DEFAULT_ALPHABET,
                8
        );

        DocumentEntity documentEntity = DocumentEntity.builder()
                .documentId(documentId)
                .fileName(source)
                .normalizedName(normalizedName)
                .version(1)
                .active(true)
                .uploadedAt(LocalDateTime.now())
                .build();

        documentRepository.save(documentEntity);

        ingestChunks(file, documentEntity);

        return UploadResponseDto.builder()
                .documentId(documentId)
                .filename(source)
                .message("Upload Successful")
                .build();
    }

    private String normalizeDocumentName(String documentName) {
        String normalizedName = documentName.trim().toLowerCase();
        if(normalizedName.endsWith(".pdf")) {
            normalizedName = normalizedName.substring(0, normalizedName.length() - 4);
        }

        return normalizedName;
    }

    @Transactional
    public UploadResponseDto update(String documentId, MultipartFile file) throws IOException {
        DocumentEntity currentDocument = documentRepository.findByDocumentIdAndActiveTrue(documentId)
                .orElseThrow(() -> new RuntimeException("Active document not found"));

        int nextVersion = documentRepository.findTopByDocumentIdOrderByVersionDesc(documentId)
                .map(DocumentEntity::getVersion)
                .filter(version -> version != null)
                .map(version -> version + 1)
                .orElse(1);

        currentDocument.setActive(false);
        documentRepository.save(currentDocument);
        documentRepository.flush();

        String source = file.getOriginalFilename();
        String normalizedName = normalizeDocumentName(source);

        DocumentEntity newDocument = DocumentEntity.builder()
                .documentId(documentId)
                .fileName(source)
                .normalizedName(normalizedName)
                .version(nextVersion)
                .active(true)
                .uploadedAt(LocalDateTime.now())
                .build();

        documentRepository.save(newDocument);

        ingestChunks(file, newDocument);
        hybridSearchRepository.deleteChunksExceptVersion(documentId, nextVersion);

        return UploadResponseDto.builder()
                .documentId(documentId)
                .filename(source)
                .message("Update Successful")
                .build();
    }

    private void ingestChunks(MultipartFile file, DocumentEntity documentEntity) throws IOException {
        var resource = new InputStreamResource(file.getInputStream());
        var pdfReader = new PagePdfDocumentReader(resource);
        List<Document> rawDocs = pdfReader.read();
        var splitter = new TokenTextSplitter();
        List<Document> chunks = splitter.split(rawDocs);

        chunks.forEach(chunk -> {
            chunk.getMetadata().put("documentId", documentEntity.getDocumentId());
            chunk.getMetadata().put("source", documentEntity.getFileName());
            chunk.getMetadata().put("normalizedName", documentEntity.getNormalizedName());
            chunk.getMetadata().put("version", documentEntity.getVersion());
            chunk.getMetadata().put("active", documentEntity.getActive());
        });

        vectorStore.add(chunks);
        hybridSearchRepository.refreshSearchableText(documentEntity.getDocumentId());
    }
}
