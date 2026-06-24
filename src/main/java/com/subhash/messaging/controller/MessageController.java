package com.subhash.messaging.controller;

import com.subhash.messaging.dto.MessageResponse;
import com.subhash.messaging.dto.SendMessageRequest;
import com.subhash.messaging.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse sendMessage(
            @RequestHeader("X-User-Id") Long senderId,
            @RequestBody @Valid SendMessageRequest request) {
        return messageService.sendMessage(senderId, request);
    }
}
