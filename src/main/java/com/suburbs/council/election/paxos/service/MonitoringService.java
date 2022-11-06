package com.suburbs.council.election.paxos.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suburbs.council.election.messages.HeartBeat;
import com.suburbs.council.election.paxos.Context;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is responsible for monitoring the connectivity status between the current node
 * and the member nodes. It sends and receives heartbeat to other members.
 */
public class MonitoringService extends Thread {
    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);
    private static final long WAIT_TIME = 5000L;

    private final Context context;
    private final ObjectMapper mapper;
    private final String heartBeatJson;
    private boolean isFirstTime = true;

    /**
     * Constructor.
     *
     * @param context Context object
     */
    public MonitoringService(Context context) throws JsonProcessingException {
        this.context = context;
        this.mapper = new ObjectMapper();
        this.heartBeatJson = mapper.writeValueAsString(new HeartBeat(context.getNodeName()));
    }

    /**
     * Iterates over the list of members and logs the current connectivity status, if
     * a connection is not active it will try to re-establish the connection with the member.
     */
    @Override
    public void run() {
        while (!Thread.interrupted()) {

            if (isFirstTime) {
                log.info("Monitoring will start after 30 seconds");
                try {
                    Thread.sleep(30000);
                    isFirstTime = false;

                } catch (InterruptedException e) {
                    log.error("[{}], Sleeping thread interrupted", context.getNodeName());
                }

            } else {

                // Print the state information
                log.info("[{}]: ----------------------------> Current state: {}",
                        context.getNodeName(), context.getCurrentState());
            }


            // Iterate over the members and send heartbeat.
            context.getMembers()
                    .forEach(member -> {

                        // If connection is not active, attempt to start a new connection
                        try {
                            if (!member.isConnected())
                                member.initializeSocket();

                            sendHeartBeat(member.socket());

                        } catch (IOException e) {
                            log.error("[{}]: Error initializing socket to host: {} and port: {}",
                                    context.getNodeName(),
                                    member.getHost(),
                                    member.getPort());
                        }
                    });

            // Poll received heartbeat messages
            pollHeartBeat();

            try {
                // Wait for WAIT_TIME ms before next check.
                Thread.sleep(WAIT_TIME);

            } catch (InterruptedException e) {
                log.error("[{}]: Error while monitoring service waiting for wait time", context.getNodeName());
            }
        }
    }

    /**
     * Logs the current status of the member node connection.
     *
     * @param nodeName Current node name
     * @param memberName Name of the member node
     * @param isActive Is the connection active
     */
    @Deprecated
    private void printMemberStatus(String nodeName, String memberName, boolean isActive) {
        log.info("[{}]: Is connection to {} active -> {}",
                nodeName,
                memberName,
                isActive
        );
    }

    /**
     * Writes the {@link HeartBeat} to socket output stream.
     *
     * @param socket Member socket
     * @throws IOException Thrown if socket is unable to open on that port
     */
    private void sendHeartBeat(Socket socket) throws IOException {

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println(heartBeatJson);
        out.close();
    }

    /**
     * Poll the buffered heartbeats received from other members.
     */
    public void pollHeartBeat() {

        List<HeartBeat> messages = context.pollHeartBeatMessage();
        if (messages.isEmpty()) return;

        messages.forEach(heartbeat -> log.trace("[{}]: Received heartbeat from member {}",
                context.getNodeName(),
                (heartbeat).getName())
        );
    }
}
