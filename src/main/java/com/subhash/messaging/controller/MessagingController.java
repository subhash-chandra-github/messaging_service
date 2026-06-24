package com.subhash.messaging.controller;

import com.subhash.messaging.common.PagedResponse;
import com.subhash.messaging.dto.ConversationResponse;
import com.subhash.messaging.dto.MessageResponse;
import com.subhash.messaging.dto.SendMessageRequest;
import com.subhash.messaging.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MessagingController {

    private final ConversationService conversationService;

    @PostMapping("/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse sendMessage(
            @RequestHeader("X-User-Id") Long senderId,
            @RequestBody @Valid SendMessageRequest request) {
        return conversationService.sendMessage(senderId, request);
    }

    @GetMapping("/conversations")
    public List<ConversationResponse> listConversations(
            @RequestHeader("X-User-Id") Long userId) {
        return conversationService.listUserConversations(userId);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public PagedResponse<MessageResponse> getMessages(
            @RequestHeader("X-User-Id") Long requesterId,
            @PathVariable Long conversationId,
            @PageableDefault(size = 20, sort = "sentAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return conversationService.getConversationMessages(requesterId, conversationId, pageable);
    }
}
