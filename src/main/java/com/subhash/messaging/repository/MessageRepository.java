package com.subhash.messaging.repository;

import com.subhash.messaging.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByConversationIdOrderBySentAtAsc(Long conversationId, Pageable pageable);

    Optional<Message> findFirstByConversationIdOrderBySentAtDesc(Long conversationId);
}
