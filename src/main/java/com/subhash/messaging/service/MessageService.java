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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MessageService {

    private final ConversationRepository conversationRepository;
    private final ParticipantsRepository participantsRepository;
    private final MessageRepository messageRepository;

    public MessageResponse sendMessage(Long senderId, SendMessageRequest request) {
        Long recipientId = request.getRecipientId();

        if (senderId.equals(recipientId)) {
            throw new BadRequestException("Cannot send a message to yourself");
        }

        List<Long> existingConversationIds =
                participantsRepository.findConversationIdsBetweenUsers(senderId, recipientId);

        Conversation conversation;
        if (existingConversationIds.isEmpty()) {
            conversation = conversationRepository.save(new Conversation());
            participantsRepository.save(new Participants(null, conversation, senderId));
            participantsRepository.save(new Participants(null, conversation, recipientId));
        } else if (existingConversationIds.size() == 1) {
            conversation = conversationRepository.findById(existingConversationIds.get(0))
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation", existingConversationIds.get(0)));
        } else {
            throw new IllegalStateException("Data integrity error: multiple conversations found between users "
                    + senderId + " and " + recipientId);
        }

        Message message = new Message();
        message.setConversation(conversation);
        message.setSenderId(senderId);
        message.setContent(request.getContent());

        return MessageMapper.toResponse(messageRepository.save(message));
    }
}
