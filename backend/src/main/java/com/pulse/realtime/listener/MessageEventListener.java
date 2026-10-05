package com.pulse.realtime.listener;

import com.pulse.messages.event.MessageCreatedEvent;
import com.pulse.messages.event.MessageDeletedEvent;
import com.pulse.messages.event.MessageUpdatedEvent;
import com.pulse.messages.event.ReactionChangedEvent;
import com.pulse.realtime.dto.MessageDeletedPayload;
import com.pulse.realtime.dto.ReactionPayload;
import com.pulse.realtime.dto.RealtimeEventType;
import com.pulse.realtime.service.RealtimePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Broadcasts only after the database transaction commits, so clients never receive
 * an event for a message that ends up rolled back.
 */
@Component
@RequiredArgsConstructor
public class MessageEventListener {

    private final RealtimePublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageCreated(MessageCreatedEvent event) {
        publisher.toChannel(event.message().channelId(), RealtimeEventType.MESSAGE_CREATED, event.message());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageUpdated(MessageUpdatedEvent event) {
        publisher.toChannel(event.message().channelId(), RealtimeEventType.MESSAGE_UPDATED, event.message());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageDeleted(MessageDeletedEvent event) {
        publisher.toChannel(event.channelId(), RealtimeEventType.MESSAGE_DELETED,
                new MessageDeletedPayload(event.channelId(), event.messageId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReactionChanged(ReactionChangedEvent event) {
        RealtimeEventType type = event.added() ? RealtimeEventType.REACTION_ADDED : RealtimeEventType.REACTION_REMOVED;
        publisher.toChannel(event.channelId(), type,
                new ReactionPayload(event.messageId(), event.userId(), event.emoji()));
    }
}
