package com.subhash.messaging.service;

import com.subhash.messaging.common.MessageMapper;
import com.subhash.messaging.common.PagedResponse;
import com.subhash.messaging.dto.ConversationResponse;
import com.subhash.messaging.dto.MessageResponse;
import com.subhash.messaging.entity.Conversation;
import com.subhash.messaging.entity.Message;
import com.subhash.messaging.entity.Participants;
import com.subhash.messaging.exception.ForbiddenException;
import com.subhash.messaging.exception.ResourceNotFoundException;
import com.subhash.messaging.repository.ConversationRepository;
import com.subhash.messaging.repository.MessageRepository;
import com.subhash.messaging.repository.ParticipantsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ParticipantsRepository participantsRepository;
    private final MessageRepository messageRepository;

    @Transactional(readOnly = true)
    public List<ConversationResponse> listUserConversations(Long userId) {
        List<Participants> userParticipations = participantsRepository.findByUserId(userId);
        if (userParticipations.isEmpty()) {
            return List.of();
        }

        List<Long> conversationIds = userParticipations.stream()
                .map(p -> p.getConversation().getId())
                .distinct()
                .toList();

        Map<Long, List<Long>> participantsByConversation = participantsRepository
                .findByConversationIdIn(conversationIds)
                .stream()
                .collect(Collectors.groupingBy(
                        p -> p.getConversation().getId(),
                        Collectors.mapping(Participants::getUserId, Collectors.toList())
                ));

        List<Conversation> conversations = conversationRepository.findAllById(conversationIds);

        return conversations.stream()
                .map(conv -> {
                    MessageResponse lastMessage = messageRepository
                            .findFirstByConversationIdOrderBySentAtDesc(conv.getId())
                            .map(MessageMapper::toResponse)
                            .orElse(null);

                    return ConversationResponse.builder()
                            .id(conv.getId())
                            .participantIds(participantsByConversation.getOrDefault(conv.getId(), List.of()))
                            .createdAt(conv.getCreatedAt())
                            .lastMessage(lastMessage)
                            .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<MessageResponse> getConversationMessages(
            Long requesterId, Long conversationId, Pageable pageable) {

        conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId));

        if (!participantsRepository.existsByConversationIdAndUserId(conversationId, requesterId)) {
            throw new ForbiddenException("Access denied to conversation " + conversationId);
        }

        return MessageMapper.toPagedResponse(
                messageRepository.findByConversationIdOrderBySentAtAsc(conversationId, pageable));
    }
}
