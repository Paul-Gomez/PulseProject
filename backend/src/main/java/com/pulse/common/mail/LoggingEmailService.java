package com.pulse.common.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Development stand-in used while pulse.mail.enabled is false: the email is printed instead of sent so the
 * verification/reset links can be copied from the console. Never leave mail disabled in production, because
 * these messages contain account links.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "pulse.mail.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingEmailService implements EmailService {

    @Override
    public void send(String to, String subject, String body) {
        log.info("[mail disabled] To: {} | Subject: {}\n{}", to, subject, body);
    }
}
