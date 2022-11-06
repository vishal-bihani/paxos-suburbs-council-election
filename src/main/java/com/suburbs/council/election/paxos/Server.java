package com.suburbs.council.election.paxos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suburbs.council.election.messages.Message;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class runs on a separate thread and is responsible for receiving requests
 * from other member nodes and dispatching them to the appropriate services.
 */
public class Server extends Thread {
    private static final Logger log = LoggerFactory.getLogger(Server.class);

    private static Context context;
    private ServerSocket serverSocket;

    /**
     * Constructor.
     *
     * @param context Context stores all the synchronized resources
     * @throws IOException Thrown if port number is invalid or socket on that port
     *          could not be opened
     */
    public Server(Context context) throws IOException {
        this.context = context;
        serverSocket = new ServerSocket(context.getServerPort());
    }

    /**
     * In order to make {@link ServerSocket} serve multiple connections, each client connection
     * has to be handled by a different {@link RequestHandler Thread}.
     */
    @Override
    public void run() {
        while (true) {
            try {
                if (Thread.interrupted()) break;

                new RequestHandler(serverSocket.accept())
                        .start();

            } catch (IOException e) {
                log.error("Error accepting connections ...");
            }
        }
    }

    /**
     * This class handles a single client connection to the {@link #serverSocket} and
     * handles the received message by delivering it to appropriate service.
     */
    private static class RequestHandler extends Thread {
        private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

        private final Socket socket;
        private final ObjectMapper objectMapper;

        /**
         * Constructor.
         *
         * @param socket Socket.
         */
        public RequestHandler(Socket socket) {
            this.socket = socket;
            this.objectMapper = new ObjectMapper();
        }

        @Override
        public void run() {
            String incomingMessage;
            try {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream())
                );
                while ((incomingMessage = in.readLine()) != null) {
                    dispatchMessage(incomingMessage);
                }

            } catch (IOException | InterruptedException e) {
                log.error(e.getMessage());
            }
        }

        private Message convertToMessage(String incomingMessage) throws IOException {
            return objectMapper.readValue(incomingMessage, Message.class);
        }

        private void dispatchMessage(String incomingMessage) throws InterruptedException, JsonProcessingException {

            Map<String, Object> message = objectMapper.readValue(incomingMessage, Map.class);
            String messageType = (String) message.get("messageType");
            Message.Type messageTypeEnum = Message.Type.valueOf(messageType);

            switch (messageTypeEnum) {
                case HEARTBEAT -> context.putHeartBeatMessages(incomingMessage);
                default -> context.putIncomingMessageToQueue(incomingMessage);
            }
        }
    }
}
