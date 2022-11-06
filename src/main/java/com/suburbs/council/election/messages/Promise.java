package com.suburbs.council.election.messages;

import com.suburbs.council.election.paxos.Context;

/**
 * Promise message is send by Member to Proposer to indicate that
 * Member will accept the request for proposed state.
 */
public class Promise implements Message {

    private String responderNodeName;
    private Type messageType;
    private int proposerNodeId;
    private String prepareMessageId;
    private Prepare lastPrepareMessage;

    /**
     * Constructor.
     *
     * @param context Context object holds the resources which are shared among all the threads
     * @param prepareMessageId Identifier of the {@link Prepare} message
     * @param proposerNodeId Node id of the Proposer node
     */
    public Promise(Context context, String prepareMessageId, int proposerNodeId) {
        messageType = Type.PROMISE;

        responderNodeName = context.getNodeName();
        this.prepareMessageId = prepareMessageId;
        this.proposerNodeId = proposerNodeId;
    }

    // No-arg constructor used by the jackson
    public Promise() {
    }

    public String getResponderNodeName() {
        return responderNodeName;
    }

    public void setResponderNodeName(String responderNodeName) {
        this.responderNodeName = responderNodeName;
    }

    public void setMessageType(Type messageType) {
        this.messageType = messageType;
    }

    public String getPrepareMessageId() {
        return prepareMessageId;
    }

    public void setPrepareMessageId(String prepareMessageId) {
        this.prepareMessageId = prepareMessageId;
    }

    public int getProposerNodeId() {
        return proposerNodeId;
    }

    public void setProposerNodeId(int proposerNodeId) {
        this.proposerNodeId = proposerNodeId;
    }

    public Prepare getLastPrepareMessage() {
        return lastPrepareMessage;
    }

    public void setLastPrepareMessage(Prepare lastPrepareMessage) {
        this.lastPrepareMessage = lastPrepareMessage;
    }

    /**
     * Get the type of the message.
     *
     * @return Type of the message
     */
    @Override
    public Type getMessageType() {
        return messageType;
    }
}
