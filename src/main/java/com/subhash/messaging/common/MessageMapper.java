package com.subhash.messaging.common;

import com.subhash.messaging.dto.MessageResponse;
import com.subhash.messaging.entity.Message;
import org.springframework.data.domain.Page;

import java.util.List;

public class MessageMapper {

    private MessageMapper() {}

    public static MessageResponse toResponse(Message message) {
        return MessageResponse.builder()
                .conversationId(message.getConversation().getId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .build();
    }

    public static PagedResponse<MessageResponse> toPagedResponse(Page<Message> page) {
        List<MessageResponse> content = page.getContent().stream()
                .map(MessageMapper::toResponse)
                .toList();
        return PagedResponse.<MessageResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
