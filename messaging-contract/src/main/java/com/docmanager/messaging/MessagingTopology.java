package com.docmanager.messaging;

/** Namen von Exchange, Routing-Keys und Queues. */
public final class MessagingTopology {

    public static final String EXCHANGE = "docmanager.exchange";

    public static final String OCR_REQUEST_KEY = "document.process";
    public static final String OCR_RESULT_KEY = "document.ocr.done";
    public static final String INDEX_KEY = "document.index";

    public static final String OCR_REQUEST_QUEUE = "document.processing";
    public static final String OCR_RESULT_QUEUE = "document.ocr.result";
    public static final String INDEX_QUEUE = "document.index";

    /** Hier landen Nachrichten, die auch nach allen Wiederholungen nicht verarbeitet werden konnten. */
    public static final String DEAD_LETTER_QUEUE = "docmanager.dead-letter";

    private MessagingTopology() {}
}
