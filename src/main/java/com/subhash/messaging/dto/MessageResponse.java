package com.subhash.messaging.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MessageResponse {
    private Long conversationId;
    private Long senderId;
    private String content;
    private LocalDateTime sentAt;
}
