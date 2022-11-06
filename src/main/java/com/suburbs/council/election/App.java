package com.suburbs.council.election;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suburbs.council.election.paxos.PaxosDriver;
import com.suburbs.council.election.utils.ConfigurationUtil;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is the starting point of this application.
 */
public class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static PaxosDriver paxosDriver;

    /**
     * main method will receive the path for configuration file with following format:
     *      <pre><code>file:///path/to/config/file.json</code></pre>
     *
     * @param args Arguments, 1st one being path of the configuration file
     * @throws IOException Thrown if something went wrong while parsing configuration file
     */
    public static void main(String[] args) throws IOException {
        log.info("Loading configuration from file: {}", args[0]);

        InputStream configurationStream;
        Node node;

        try {
            // Parsing the configuration file and mapping it to the Node object
            configurationStream = ConfigurationUtil.loadConfigurationFromFile(args[0]);
            node = mapper
                    .readValue(configurationStream, Node.class);

            // Remove the node information from the members list, as the downstream components
            // will try to make a connection with the members
            removeNodeFromMemberList(node.getName(), node.getMembers());

        } catch (IOException e) {
            throw new IOException("Unable to parse the configuration file with error: " + e.getMessage());
        }

        // Paxos driver initialization. PaxosDriver performs all the paxos related logic.
        paxosDriver = new PaxosDriver(node);
        paxosDriver.start();
    }

    /**
     * Removes current nodes details from the member list.
     *
     * @param nodeName Current node name
     * @param members list of members
     */
    private static void removeNodeFromMemberList(String nodeName, List<Member> members) {
        members.removeIf(member -> nodeName.equalsIgnoreCase(member.getName()));
    }
}