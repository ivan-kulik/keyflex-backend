package com.keyflex.common.mail;

public interface EmailSender {
    void send(String to, String subject, String body);
}
