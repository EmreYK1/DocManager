package com.docmanager.ocr.messaging;

import com.docmanager.messaging.OcrRequestMessage;
import com.docmanager.messaging.OcrResultMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;

/**
 * Greift, wenn alle Wiederholungen gescheitert sind: Der Auftrag kommt in die Dead-Letter-Queue, und das
 * Dokument wird als fehlgeschlagen gemeldet, damit es nicht für immer auf die Texterkennung wartet.
 */
@Slf4j
public class FailedOcrRecoverer implements MessageRecoverer {

    private final MessageRecoverer deadLetter;
    private final MessageConverter converter;
    private final OcrResultPublisher publisher;

    public FailedOcrRecoverer(MessageRecoverer deadLetter, MessageConverter converter, OcrResultPublisher publisher) {
        this.deadLetter = deadLetter;
        this.converter = converter;
        this.publisher = publisher;
    }

    @Override
    public void recover(Message message, Throwable cause) {
        deadLetter.recover(message, cause);
        message.getMessageProperties().setInferredArgumentType(OcrRequestMessage.class);
        if (converter.fromMessage(message) instanceof OcrRequestMessage request) {
            log.error("Texterkennung endgültig gescheitert: id={}", request.documentId());
            publisher.publish(OcrResultMessage.failed(request.documentId(), "Verarbeitung gescheitert"));
        }
    }
}
