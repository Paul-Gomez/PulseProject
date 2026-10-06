package com.pulse.identity;

import com.pulse.common.mail.EmailService;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class CapturingEmailService implements EmailService {

    record Mail(String to, String subject, String body) {
    }

    private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");

    private final List<Mail> sent = new CopyOnWriteArrayList<>();

    @Override
    public void send(String to, String subject, String body) {
        sent.add(new Mail(to, subject, body));
    }

    List<Mail> sentTo(String email) {
        return sent.stream().filter(mail -> mail.to().equals(email)).toList();
    }

    Optional<String> lastTokenSentTo(String email) {
        List<Mail> mails = sentTo(email);
        if (mails.isEmpty()) {
            return Optional.empty();
        }
        Matcher matcher = TOKEN.matcher(mails.get(mails.size() - 1).body());
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }
}
