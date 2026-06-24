Build the backend for one-to-one messaging: sending messages, reading conversation history, and
listing a user’s conversations. A REST API is perfectly fine for this exercise.
Your service should provide:
    Send a message between two users.
    Fetch a conversation’s history, paginated.
    List the conversations a given user is part of.
    Persist messages with sensible ordering, and enforce who is allowed to read which conversation.
What a strong service demonstrates
    History pagination stays stable even as new messages arrive (no duplicates or skipped messages).
    A user cannot read a conversation they are not part of.
    Send, paginated fetch, and an authorization-denied read are all covered by tests.