package com.subhash.messaging.repository;

import com.subhash.messaging.entity.Message;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdAndIdGreaterThanOrderBySentAtAsc(Long conversationId, Long afterId, Limit limit);

    Optional<Message> findFirstByConversationIdOrderBySentAtDesc(Long conversationId);
}
