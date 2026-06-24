package com.subhash.messaging.controller;

import com.subhash.messaging.common.CursorPageResponse;
import com.subhash.messaging.dto.ConversationResponse;
import com.subhash.messaging.dto.MessageResponse;
import com.subhash.messaging.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping
    public List<ConversationResponse> listConversations(
            @RequestHeader("X-User-Id") Long userId) {
        return conversationService.listUserConversations(userId);
    }

    @GetMapping("/{conversationId}/messages")
    public CursorPageResponse<MessageResponse> getMessages(
            @RequestHeader("X-User-Id") Long requesterId,
            @PathVariable Long conversationId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        return conversationService.getConversationMessages(requesterId, conversationId, cursor, size);
    }
}
