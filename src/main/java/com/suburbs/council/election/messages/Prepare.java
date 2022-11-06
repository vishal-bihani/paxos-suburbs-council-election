package com.suburbs.council.election.messages;

import com.suburbs.council.election.paxos.Context;

public class Prepare implements Message {

    private int proposerNodeId;
    private String proposerNodeName;
    private Type messageType;
    private Proposal proposal;
    private String newPrepareMessageId;

    public Prepare(Context context) {
        this.messageType = Type.PREPARE;

        this.proposerNodeId = context.getNodeId();
        this.proposerNodeName = context.getNodeName();
        this.proposal = new Proposal("Leader -> " + context.getNodeName());
        this.newPrepareMessageId = context.getNewProposalNumber();
    }

    public Prepare() {
    }

    public void setMessageType(Type messageType) {
        this.messageType = messageType;
    }

    public Proposal getProposal() {
        return proposal;
    }

    public void setProposal(Proposal proposal) {
        this.proposal = proposal;
    }

    public String getNewPrepareMessageId() {
        return newPrepareMessageId;
    }

    public void setNewPrepareMessageId(String newPrepareMessageId) {
        this.newPrepareMessageId = newPrepareMessageId;
    }

    public String getProposerNodeName() {
        return proposerNodeName;
    }

    public void setProposerNodeName(String proposerNodeName) {
        this.proposerNodeName = proposerNodeName;
    }

    public int getProposerNodeId() {
        return proposerNodeId;
    }

    public void setProposerNodeId(int proposerNodeId) {
        this.proposerNodeId = proposerNodeId;
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
