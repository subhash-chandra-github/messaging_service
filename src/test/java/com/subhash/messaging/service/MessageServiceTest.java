package com.subhash.messaging.service;

import com.subhash.messaging.dto.MessageResponse;
import com.subhash.messaging.dto.SendMessageRequest;
import com.subhash.messaging.exception.BadRequestException;
import com.subhash.messaging.repository.ConversationRepository;
import com.subhash.messaging.repository.MessageRepository;
import com.subhash.messaging.repository.ParticipantsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class MessageServiceTest {

    @Autowired MessageService messageService;
    @Autowired ConversationRepository conversationRepository;
    @Autowired ParticipantsRepository participantsRepository;
    @Autowired MessageRepository messageRepository;

    @Test
    void sendMessage_createsNewConversation_whenNoneExists() {
        MessageResponse response = send(1L, 2L, "Hello!");

        assertThat(response.getConversationId()).isNotNull();
        assertThat(messageRepository.count()).isEqualTo(1);
        assertThat(response.getSenderId()).isEqualTo(1L);
        assertThat(response.getContent()).isEqualTo("Hello!");
        assertThat(response.getSentAt()).isNotNull();

        assertThat(conversationRepository.count()).isEqualTo(1);
        assertThat(participantsRepository.count()).isEqualTo(2);
    }

    @Test
    void sendMessage_reusesExistingConversation_onSubsequentMessages() {
        MessageResponse first = send(1L, 2L, "First message");
        MessageResponse second = send(1L, 2L, "Second message");

        assertThat(second.getConversationId()).isEqualTo(first.getConversationId());
        assertThat(conversationRepository.count()).isEqualTo(1);
        assertThat(participantsRepository.count()).isEqualTo(2);
    }

    @Test
    void sendMessage_reusesExistingConversation_whenReplyingBack() {
        MessageResponse first = send(1L, 2L, "Hey");
        MessageResponse reply = send(2L, 1L, "Hi back");

        assertThat(reply.getConversationId()).isEqualTo(first.getConversationId());
        assertThat(conversationRepository.count()).isEqualTo(1);
    }

    @Test
    void sendMessage_createsSeparateConversations_forDifferentUserPairs() {
        MessageResponse conv1 = send(1L, 2L, "Hello user 2");
        MessageResponse conv2 = send(1L, 3L, "Hello user 3");

        assertThat(conv1.getConversationId()).isNotEqualTo(conv2.getConversationId());
        assertThat(conversationRepository.count()).isEqualTo(2);
    }

    @Test
    void sendMessage_throwsBadRequest_whenSenderEqualsRecipient() {
        assertThatThrownBy(() -> send(1L, 1L, "talking to myself"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot send a message to yourself");
    }

    // --- helpers ---

    private MessageResponse send(Long from, Long to, String content) {
        SendMessageRequest req = new SendMessageRequest();
        req.setRecipientId(to);
        req.setContent(content);
        return messageService.sendMessage(from, req);
    }
}
