package com.suburbs.council.election.messages;

import java.io.Serializable;

/**
 * Messages are communicated between member nodes.
 */
public interface Message extends Serializable {

    /**
     * Get the type of the message.
     *
     * @return Type of the message
     */
    Type getMessageType();

    enum Type {
        HEARTBEAT,
        PREPARE,
        PROMISE,
        ACCEPT,
        REJECT,
        ACCEPTED
    }
}
