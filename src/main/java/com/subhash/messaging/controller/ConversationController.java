package com.subhash.messaging.controller;

import com.subhash.messaging.common.PagedResponse;
import com.subhash.messaging.dto.ConversationResponse;
import com.subhash.messaging.dto.MessageResponse;
import com.subhash.messaging.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
    public PagedResponse<MessageResponse> getMessages(
            @RequestHeader("X-User-Id") Long requesterId,
            @PathVariable Long conversationId,
            @PageableDefault(size = 20, sort = "sentAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return conversationService.getConversationMessages(requesterId, conversationId, pageable);
    }
}
