package com.subhash.messaging.service;

import com.subhash.messaging.common.CursorPageResponse;
import com.subhash.messaging.dto.ConversationResponse;
import com.subhash.messaging.dto.MessageResponse;
import com.subhash.messaging.dto.SendMessageRequest;
import com.subhash.messaging.exception.ForbiddenException;
import com.subhash.messaging.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ConversationServiceTest {

    @Autowired ConversationService conversationService;
    @Autowired MessageService messageService;

    private Long conversationId;

    @BeforeEach
    void setUp() {
        // Seed a conversation between user 1 and user 2 with 5 messages
        conversationId = send(1L, 2L, "msg 1").getConversationId();
        send(2L, 1L, "msg 2");
        send(1L, 2L, "msg 3");
        send(2L, 1L, "msg 4");
        send(1L, 2L, "msg 5");
    }

    // --- getConversationMessages ---

    @Test
    void getMessages_returnsFirstPage_withoutCursor() {
        CursorPageResponse<MessageResponse> page = conversationService
                .getConversationMessages(1L, conversationId, null, 3);

        assertThat(page.getMessages()).hasSize(3);
        assertThat(page.isHasMore()).isTrue();
        assertThat(page.getNextCursor()).isNotNull();
        assertThat(page.getMessages()).extracting(MessageResponse::getContent)
                .containsExactly("msg 1", "msg 2", "msg 3");
    }

    @Test
    void getMessages_returnsNextPage_withCursor() {
        CursorPageResponse<MessageResponse> firstPage = conversationService
                .getConversationMessages(1L, conversationId, null, 3);

        CursorPageResponse<MessageResponse> secondPage = conversationService
                .getConversationMessages(1L, conversationId, firstPage.getNextCursor(), 3);

        assertThat(secondPage.getMessages()).hasSize(2);
        assertThat(secondPage.isHasMore()).isFalse();
        assertThat(secondPage.getNextCursor()).isNull();
        assertThat(secondPage.getMessages()).extracting(MessageResponse::getContent)
                .containsExactly("msg 4", "msg 5");
    }

    @Test
    void getMessages_hasMoreFalse_whenAllFitInOnePage() {
        CursorPageResponse<MessageResponse> page = conversationService
                .getConversationMessages(1L, conversationId, null, 10);

        assertThat(page.getMessages()).hasSize(5);
        assertThat(page.isHasMore()).isFalse();
        assertThat(page.getNextCursor()).isNull();
    }

    @Test
    void getMessages_returnsEmpty_whenConversationHasNoMessages() {
        // create a fresh conversation by sending one message then check history after
        // (edge case: conversation exists but cursor is past all messages)
        CursorPageResponse<MessageResponse> allMessages = conversationService
                .getConversationMessages(1L, conversationId, null, 10);

        Long lastId = allMessages.getMessages().get(allMessages.getMessages().size() - 1).getId();

        CursorPageResponse<MessageResponse> beyondEnd = conversationService
                .getConversationMessages(1L, conversationId, lastId, 10);

        assertThat(beyondEnd.getMessages()).isEmpty();
        assertThat(beyondEnd.isHasMore()).isFalse();
        assertThat(beyondEnd.getNextCursor()).isNull();
    }

    @Test
    void getMessages_messagesAreOrderedBySentAtAsc() {
        CursorPageResponse<MessageResponse> page = conversationService
                .getConversationMessages(1L, conversationId, null, 10);

        List<MessageResponse> messages = page.getMessages();
        for (int i = 1; i < messages.size(); i++) {
            assertThat(messages.get(i).getSentAt())
                    .isAfterOrEqualTo(messages.get(i - 1).getSentAt());
        }
    }

    @Test
    void getMessages_throwsForbidden_forNonParticipant() {
        assertThatThrownBy(() ->
                conversationService.getConversationMessages(99L, conversationId, null, 10))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void getMessages_throwsNotFound_forMissingConversation() {
        assertThatThrownBy(() ->
                conversationService.getConversationMessages(1L, 999L, null, 10))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Conversation not found");
    }

    // --- listUserConversations ---

    @Test
    void listConversations_returnsEmpty_forNewUser() {
        List<ConversationResponse> result = conversationService.listUserConversations(42L);
        assertThat(result).isEmpty();
    }

    @Test
    void listConversations_returnsConversation_withCorrectParticipants() {
        List<ConversationResponse> result = conversationService.listUserConversations(1L);

        assertThat(result).hasSize(1);
        ConversationResponse conv = result.get(0);
        assertThat(conv.getId()).isEqualTo(conversationId);
        assertThat(conv.getParticipantIds()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void listConversations_includesLastMessage() {
        List<ConversationResponse> result = conversationService.listUserConversations(1L);

        MessageResponse lastMessage = result.get(0).getLastMessage();
        assertThat(lastMessage).isNotNull();
        assertThat(lastMessage.getContent()).isEqualTo("msg 5");
    }

    @Test
    void listConversations_returnsAllConversationsForUser() {
        // user 1 starts a second conversation with user 3
        send(1L, 3L, "hey user 3");

        List<ConversationResponse> result = conversationService.listUserConversations(1L);
        assertThat(result).hasSize(2);
    }

    @Test
    void listConversations_isolatesConversationsBetweenDifferentPairs() {
        send(2L, 3L, "side conversation");

        // user 3 should only see the conversation with user 2, not user 1's conversation
        List<ConversationResponse> user3Convs = conversationService.listUserConversations(3L);
        assertThat(user3Convs).hasSize(1);
        assertThat(user3Convs.get(0).getParticipantIds()).containsExactlyInAnyOrder(2L, 3L);
    }

    // --- helpers ---

    private MessageResponse send(Long from, Long to, String content) {
        SendMessageRequest req = new SendMessageRequest();
        req.setRecipientId(to);
        req.setContent(content);
        return messageService.sendMessage(from, req);
    }
}
