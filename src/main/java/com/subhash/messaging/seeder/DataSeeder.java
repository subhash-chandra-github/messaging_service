package com.subhash.messaging.seeder;

import com.subhash.messaging.dto.SendMessageRequest;
import com.subhash.messaging.repository.ConversationRepository;
import com.subhash.messaging.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final MessageService messageService;
    private final ConversationRepository conversationRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (conversationRepository.count() > 0) {
            log.info("Data already seeded, skipping.");
            return;
        }

        log.info("Seeding conversation data...");

        // Conversation: User 1 ↔ User 2
        send(1L, 2L, "Hey! Are you free this weekend?");
        send(2L, 1L, "Yeah, what's up?");
        send(1L, 2L, "Want to catch a movie?");
        send(2L, 1L, "Sounds great, let's do Saturday.");
        send(1L, 2L, "Perfect, I'll book the tickets.");

        // Conversation: User 1 ↔ User 3
        send(1L, 3L, "Hi! Did you get the project files?");
        send(3L, 1L, "Yes, reviewing them now.");
        send(1L, 3L, "Let me know if you have any questions.");

        // Conversation: User 2 ↔ User 3
        send(2L, 3L, "Can you review my PR?");
        send(3L, 2L, "On it, give me 10 minutes.");

        // Conversation: User 3 ↔ User 4
        send(3L, 4L, "Meeting rescheduled to 3pm.");
        send(4L, 3L, "Got it, thanks for the heads up.");
        send(3L, 4L, "See you there.");

        // Conversation: User 5 ↔ User 6 — 50 messages for cursor pagination testing
        send(5L, 6L, "[1] Hey, just pushed the first draft of the API spec.");
        send(6L, 5L, "[2] Nice, I'll take a look now.");
        send(5L, 6L, "[3] The auth section is still incomplete, just FYI.");
        send(6L, 5L, "[4] Got it, I'll skip that for now.");
        send(5L, 6L, "[5] Also the pagination design is cursor-based, not offset.");
        send(6L, 5L, "[6] Makes sense, offset breaks under concurrent inserts.");
        send(5L, 6L, "[7] Exactly. I used message id as the cursor.");
        send(6L, 5L, "[8] Simple and stable, good call.");
        send(5L, 6L, "[9] Did you check the participants query?");
        send(6L, 5L, "[10] Yeah, the GROUP BY HAVING COUNT trick is clever.");
        send(5L, 6L, "[11] It ensures no duplicate conversations between the same two users.");
        send(6L, 5L, "[12] What about race conditions on first message?");
        send(5L, 6L, "[13] The unique constraint on participants handles that.");
        send(6L, 5L, "[14] Right, constraint violation would roll back the transaction.");
        send(5L, 6L, "[15] We could add retry logic later if needed.");
        send(6L, 5L, "[16] Let's keep it simple for now.");
        send(5L, 6L, "[17] Agreed. Did you test the 403 on non-participants?");
        send(6L, 5L, "[18] Yes, it correctly rejects unknown user IDs.");
        send(5L, 6L, "[19] What size did you use for the page in your tests?");
        send(6L, 5L, "[20] I used size=10, gave me 5 pages for this conversation.");
        send(5L, 6L, "[21] Perfect for testing hasMore flipping to false.");
        send(6L, 5L, "[22] Last page had 0 messages after cursor, hasMore=false.");
        send(5L, 6L, "[23] That edge case is important to cover.");
        send(6L, 5L, "[24] Already have a test for cursor past the last message.");
        send(5L, 6L, "[25] Good. How's the list conversations endpoint looking?");
        send(6L, 5L, "[26] Returns all conversations with the last message preview.");
        send(5L, 6L, "[27] Does it handle users with zero conversations?");
        send(6L, 5L, "[28] Yes, returns an empty list.");
        send(5L, 6L, "[29] And the lastMessage field is null for empty conversations?");
        send(6L, 5L, "[30] There can't be empty conversations by design, but yes, it's null-safe.");
        send(5L, 6L, "[31] True, a conversation only gets created when a message is sent.");
        send(6L, 5L, "[32] Right, no orphan conversations.");
        send(5L, 6L, "[33] Should we add soft deletes later?");
        send(6L, 5L, "[34] Out of scope for now, but worth noting.");
        send(5L, 6L, "[35] Added a TODO comment in the spec.");
        send(6L, 5L, "[36] Cool. What about message editing or deletion?");
        send(5L, 6L, "[37] Also out of scope, this is read/write only.");
        send(6L, 5L, "[38] Fair, keeps the model clean.");
        send(5L, 6L, "[39] One thing I noticed: we don't validate content length.");
        send(6L, 5L, "[40] Good catch, we should cap it at maybe 2000 chars.");
        send(5L, 6L, "[41] I'll add a @Size annotation to the DTO.");
        send(6L, 5L, "[42] Don't forget to add a test for oversized content.");
        send(5L, 6L, "[43] Will do. Anything else before I open the PR?");
        send(6L, 5L, "[44] Double-check the README curls match the new cursor response shape.");
        send(5L, 6L, "[45] Already updated, using nextCursor and hasMore now.");
        send(6L, 5L, "[46] Perfect. I'll approve once I finish reviewing.");
        send(5L, 6L, "[47] Take your time, I'm working on another ticket meanwhile.");
        send(6L, 5L, "[48] PR looks good overall, left two minor comments.");
        send(5L, 6L, "[49] Addressed both, pushed the fixes.");
        send(6L, 5L, "[50] Looks great, approving and merging!");

        log.info("Seeding complete: 5 conversations, 63 messages (incl. 50-message pagination test conversation between users 5 and 6).");
    }

    private void send(Long from, Long to, String content) {
        SendMessageRequest req = new SendMessageRequest();
        req.setRecipientId(to);
        req.setContent(content);
        messageService.sendMessage(from, req);
    }
}
