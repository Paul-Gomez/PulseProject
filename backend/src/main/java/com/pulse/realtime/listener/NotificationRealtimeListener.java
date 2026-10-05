package com.pulse.realtime.listener;

import com.pulse.notifications.event.NotificationCreatedEvent;
import com.pulse.realtime.dto.RealtimeEventType;
import com.pulse.realtime.service.RealtimePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationRealtimeListener {

    private final RealtimePublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        publisher.toUser(event.userId(), RealtimeEventType.NOTIFICATION_CREATED, event.notification());
    }
}
