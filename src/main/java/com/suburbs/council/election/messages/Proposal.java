package com.suburbs.council.election.messages;

import java.io.Serializable;

/**
 * Proposal contains the proposed state change by the Proposer
 */
public class Proposal implements Serializable {

    private String proposedMessage;

    /**
     * Constructor.
     *
     * @param proposedMessage Proposed state change
     */
    public Proposal(String proposedMessage) {
        this.proposedMessage = proposedMessage;
    }

    // No-args used by the Jackson
    public Proposal() {
    }

    public String getProposedMessage() {
        return proposedMessage;
    }

    public void setProposedMessage(String proposedMessage) {
        this.proposedMessage = proposedMessage;
    }
}
