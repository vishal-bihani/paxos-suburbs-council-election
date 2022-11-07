# paxos-suburbs-council-election

## Configuration

Before starting the cluster, the config file should contain the
details of all the nodes (either online or offline).

Below is the explanation of each config:

1. `id`: Id of the node
2. `name`: Name of the node
3. `initProposeDelay`: Delay in seconds before initiating election
   just after startup
4. `port`: Port to which this node will bind to
5. `responseTiming`: Below are the categories of members:
   6. `IMMEDIATE`: No delay in processing events
   7. `MEDIUM`: Delay of 200 ms to 2 seconds
   8. `LATE`: Delay of 200 ms to 5 seconds
   9. `NEVER`: Max long value
10. `profile`: Below are the categories of profiles:
   11. `FOLLOWER`: Participates in voting but never initiates election
   12. `CANDIDATE`: Participates in voting and can initiate election

   > Sample config files for each node can be found in `config` directory

   > If the cluster is going to have `N` nodes then the `members` key should contain the connection 
   > details of all the `N` nodes


## Steps to run application
To run and simulate the election follow the below steps:

1. Run this command (Assuming Linux system)
    ```bash
    ./mvnw clean install
    ```
   > This will create the jar in the `target` directory
   

2. All the nodes run separately and bind to different socket.
3. Open `N` no. of terminals for `N` no. of nodes
4. Execute the following command:

   ```bash
   java -jar target/paxos-suburbs-council-election-1.0-SNAPSHOT.jar file:///path/to/config/file_for_that_node.json
   ```

   > Substitute the location of config file of that node in the 1st argument


## Conditions which are satisfied

1. Paxos implementation should work when two candidates initiate election at the same time.
2. Paxos implementation should work when all the members have `IMMEDIATE` response times
3. Paxos implementation should work when all them members have various `PROFILES` and few `CANDIDATES`
stops or disconnects after `PREPARE` phase
4. Few things to observe in logs to confirm that algorithm works:

   a. All the nodes must have same state (eventually after some delay due to different response times)
   
   b. All the `PREPARE`, `PREPROMISE,` `PROMISE`, `REJECT`, `ACCEPT`, `PREACCEPTED`, `ACCEPTED` phases are properly logged from which node to which node.
   
   c. Nodes with `LATE` response times eventually becomes consistent with other members

5. All the methods in the code is documented for better understanding of the flow
6. Paxos algorithm works with `N` number of nodes with all the 4 response time categories
7. Fast Byzantine Paxos implementation that works when members lie, collude, or intentionally do
   not participate in some voting queries but participate in others
   
   a. Confirmation regarding working of Byzantine algorithm can be made by observing logs.
   
   b. As per the algorithm, to handle `M` byzantine faults you need to have minimum of 5M+1 nodes.

   c. If a malicious candidate lies, it won't be taken into consideration as majority is achieved by correct nodes.

   d. If a malicious candidate does not participate does not participate, consensus will still be achieved as no. of correct nodes are in majority.

   e. `PREPROMISE` and `PREACCEPT` messages are used to majority votes on the intention of the proposer, so if
the malicious node tries to corrupt the state, it can't because the majority will never be achieved.

   >       Important node: All the nodes send `heartbeats` to each other. For keeping logs clean only 
   >       `info` logs are displayed. To view the `heartbeats` please change the log level from `trace`
   >       to `info` in the source code.