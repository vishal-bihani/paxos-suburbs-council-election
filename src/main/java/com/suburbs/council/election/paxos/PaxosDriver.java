package com.suburbs.council.election.paxos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.suburbs.council.election.Member;
import com.suburbs.council.election.Node;
import com.suburbs.council.election.paxos.service.MonitoringService;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles all the paxos related logic.
 */
public class PaxosDriver {
    private static final Logger log = LoggerFactory.getLogger(PaxosDriver.class);

    private final Node node;
    private final List<Member> members;
    private final Context context;
    private final PaxosMember member;

    public PaxosDriver(Node node) {
        this.node = node;
        this.members = node.getMembers();

        // Context stores all the synchronized resources and will be shared among multiple threads
        this.context = new Context(node, members);

        switch (node.getProfile()) {
            case FOLLOWER -> this.member = new Follower(context);
            case CANDIDATE -> this.member = new Candidate(context);
            default -> throw new IllegalArgumentException(node.getName() + ": Unsupported profile -> "
                    + node.getProfile());
        }

        this.members
                .forEach(nodeMember -> {
                    try {
                        nodeMember.initializeSocket();

                    } catch (IOException e) {
                        log.error("[{}]: Error initializing socket for member: {} on host: {} and port: {}",
                                node.getName(),
                                nodeMember.getName(),
                                nodeMember.getHost(),
                                nodeMember.getPort());
                        log.error("[{}]: Error: {}", node.getName(), e.getMessage());
                    }
                });


    }

    public void start() throws IOException {
        startServer();
        startMonitoringService();
        startPaxosMember();
    }

    /**
     * Starts {@link MonitoringService}.
     */
    private void startMonitoringService() throws JsonProcessingException {
        MonitoringService monitoringService = new MonitoringService(context);
        context.setMonitoringService(monitoringService);

        monitoringService.start();
    }

    /**
     * Starts {@link Server}.
     *
     * @throws IOException Thrown if server socket could not be open on the provided port
     */
    private void startServer() throws IOException {
        Server server = new Server(context);
        context.setServer(server);

        server.start();
    }

    private void startPaxosMember() {
        member.start();
    }
}
