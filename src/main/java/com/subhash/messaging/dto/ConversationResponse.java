package com.subhash.messaging.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ConversationResponse {
    private Long id;
    private List<Long> participantIds;
    private LocalDateTime createdAt;
    private MessageResponse lastMessage;
}
