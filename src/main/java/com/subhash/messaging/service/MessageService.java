package com.subhash.messaging.service;

import com.subhash.messaging.common.MessageMapper;
import com.subhash.messaging.dto.MessageResponse;
import com.subhash.messaging.dto.SendMessageRequest;
import com.subhash.messaging.entity.Conversation;
import com.subhash.messaging.entity.Message;
import com.subhash.messaging.entity.Participants;
import com.subhash.messaging.exception.BadRequestException;
import com.subhash.messaging.exception.ResourceNotFoundException;
import com.subhash.messaging.repository.ConversationRepository;
import com.subhash.messaging.repository.MessageRepository;
import com.subhash.messaging.repository.ParticipantsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MessageService {

    private final ConversationRepository conversationRepository;
    private final ParticipantsRepository participantsRepository;
    private final MessageRepository messageRepository;

    public MessageResponse sendMessage(Long senderId, SendMessageRequest request) {
        Long recipientId = request.getRecipientId();
        log.debug("sendMessage: senderId={}, recipientId={}", senderId, recipientId);

        if (senderId.equals(recipientId)) {
            log.warn("sendMessage rejected: sender and recipient are the same user ({})", senderId);
            throw new BadRequestException("Cannot send a message to yourself");
        }

        List<Long> existingConversationIds =
                participantsRepository.findConversationIdsBetweenUsers(senderId, recipientId);

        Conversation conversation;
        if (existingConversationIds.isEmpty()) {
            conversation = conversationRepository.save(new Conversation());
            participantsRepository.save(new Participants(null, conversation, senderId));
            participantsRepository.save(new Participants(null, conversation, recipientId));
            log.info("New conversation created: conversationId={}, participants=[{}, {}]",
                    conversation.getId(), senderId, recipientId);
        } else if (existingConversationIds.size() == 1) {
            Long conversationId = existingConversationIds.get(0);
            conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId));
            log.debug("Reusing existing conversationId={}", conversationId);
        } else {
            log.error("Data integrity error: {} conversations found between users {} and {}",
                    existingConversationIds.size(), senderId, recipientId);
            throw new IllegalStateException("Data integrity error: multiple conversations found between users "
                    + senderId + " and " + recipientId);
        }

        Message message = new Message();
        message.setConversation(conversation);
        message.setSenderId(senderId);
        message.setContent(request.getContent());

        MessageResponse response = MessageMapper.toResponse(messageRepository.save(message));
        log.info("Message sent: messageId={}, conversationId={}, senderId={}, recipientId={}",
                response.getId(), response.getConversationId(), senderId, recipientId);
        return response;
    }
}
