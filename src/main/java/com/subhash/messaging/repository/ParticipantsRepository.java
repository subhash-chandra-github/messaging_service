package com.subhash.messaging.repository;

import com.subhash.messaging.entity.Participants;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ParticipantsRepository extends JpaRepository<Participants, Long> {

    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);

    List<Participants> findByUserId(Long userId);

    List<Participants> findByConversationIdIn(List<Long> conversationIds);

    @Query("""
            SELECT p.conversation.id FROM Participants p
            WHERE p.userId IN (:a, :b)
            GROUP BY p.conversation.id
            HAVING COUNT(DISTINCT p.userId) = 2
            """)
    List<Long> findConversationIdsBetweenUsers(@Param("a") Long a, @Param("b") Long b);
}
