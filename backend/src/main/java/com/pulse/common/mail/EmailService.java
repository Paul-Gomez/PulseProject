package com.pulse.common.mail;

public interface EmailService {

    void send(String to, String subject, String body);
}
