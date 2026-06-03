package com.example.standardRag.repository;


import com.example.standardRag.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {

    Optional<DocumentEntity> findByDocumentIdAndUserId(String documentId, String userId);

    Optional<DocumentEntity> findByDocumentIdAndUserIdAndActiveTrue(String documentId, String userId);

    Optional<DocumentEntity> findTopByDocumentIdAndUserIdOrderByVersionDesc(String documentId, String userId);
}
