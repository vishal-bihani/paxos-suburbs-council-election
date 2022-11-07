package com.suburbs.council.election.paxos;

/**
 * Malicious Candidates are those Members who performs arbitrary operations. They can initiate election,
 * and can vote to an existing one.
 */
public class MaliciousCandidate extends Candidate {


    private Context context;

    /**
     * Constructor.
     *
     * @param context Context object holds the resources which are shared among all the threads
     */
    public MaliciousCandidate(Context context) {
        super(context);
        this.context = context;
    }
}
