package com.pulse.realtime.listener;

import com.pulse.conversations.event.DirectMessageCreatedEvent;
import com.pulse.conversations.event.DirectMessageDeletedEvent;
import com.pulse.realtime.dto.DirectMessageDeletedPayload;
import com.pulse.realtime.dto.RealtimeEventType;
import com.pulse.realtime.service.RealtimePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class DirectMessageRealtimeListener {

    private final RealtimePublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCreated(DirectMessageCreatedEvent event) {
        publisher.toConversation(event.message().conversationId(), RealtimeEventType.DIRECT_MESSAGE_CREATED, event.message());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDeleted(DirectMessageDeletedEvent event) {
        publisher.toConversation(event.conversationId(), RealtimeEventType.DIRECT_MESSAGE_DELETED,
                new DirectMessageDeletedPayload(event.conversationId(), event.messageId()));
    }
}
