package com.subhash.messaging.common;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CursorPageResponse<T> {
    private List<T> messages;
    private Long nextCursor;
    private boolean hasMore;
}
