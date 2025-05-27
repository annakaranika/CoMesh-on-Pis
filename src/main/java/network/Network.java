package network;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import javafx.geometry.Point3D;
import javafx.util.Pair;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import com.google.gson.Gson;

import kgroup.KMemberMix;
import metrics.NetworkMetric;
import network.dijkstra.*;
import network.message.*;
import network.message.payload.MessagePayload;
import network.netty.client.*;
import network.netty.server.*;

public class Network {
    private static boolean debug;
    private static LoggerStatus loggerStatus = LoggerStatus.INIT;
    public static final Object loggerLock = new Object(), memLock = new Object(), seqNoLock = new Object();
    private static FileWriter logger;
    private static final Map<String, Node> neighbors = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> failedNodes = new ConcurrentHashMap<>();
    private static final Map<String, Pair<Membership, Integer>> lastMembershipTable = new ConcurrentHashMap<>();
    private static String myIP = null, myID = null, memTableFn;
    private static final Map<String, Pair<String, Double>> routingTable = new ConcurrentHashMap<>();
    public static final Deque<Message<? extends MessagePayload>> msgBuffer = new ConcurrentLinkedDeque<>();
    private static int _seqNo = -1;
    private static KMemberMix kMemberMix;
    private static long initTS;

    private final static Map<String, Point3D> locations = new HashMap<>();

    public static final NetworkMetric metric = new NetworkMetric();

    private static int myPort = 11111;
    private static NettyServer nettyServer;

    public Network(
            String devVirtualID, String topologyFn, String devLocsFn, String memTableFnV,
            boolean debugV, FileWriter loggerV, long initTSval) {
        memTableFn = memTableFnV;
        debug = debugV;
        synchronized (loggerLock) {
            logger = loggerV;
            loggerStatus = LoggerStatus.ACTIVE;
        }
        initTS = initTSval;

        myIP = findMyIP();
        myID = getMyID(devVirtualID);
        myPort = getNodePortVirt(devVirtualID);

        // Start listening on Port
        try {
            nettyServer = new NettyServer(MessagePipelineFactory.class);
            nettyServer.startup(myPort);
        } catch (Exception e) {
            log("Cannot listen to port: " + myPort, true);
            e.printStackTrace();
            System.exit(-1);
        }

        File locationFile = new File(devLocsFn);
        try (Scanner sc = new Scanner(locationFile)) {
            while (sc.hasNextLine()) {
                String currLine = sc.nextLine();
                String[] currLines = currLine.split(" ");
                String nodeName = currLine.split(" ")[0];
                double z = 0;
                if (currLines.length >= 5) {
                    try {
                        z = Double.parseDouble(currLines[3]);
                    } catch (NumberFormatException e) {
                        z = 0.0;
                    }
                }
                Point3D pointLoc = new Point3D(
                        Double.parseDouble(currLines[1]), Double.parseDouble(currLines[2]), z);
                locations.put(nodeName, pointLoc);
            }
        } catch (FileNotFoundException ex) {
            log("Location file " + devLocsFn + " does not exist", true);
            System.exit(-1);
        }

        if (!locations.containsKey(myID)) {
            log("Not a device in the topology:\n" + locations);
            System.exit(0);
        }

        File topologyFile = new File(topologyFn);
        Graph topology = new Graph();
        String curNode;
        try (Scanner sc = new Scanner(topologyFile)) {
            // create every node in the network
            while (sc.hasNextLine()) {
                curNode = sc.nextLine().split(" ")[0];
                neighbors.put(curNode, new Node(curNode));
            }
        } catch (FileNotFoundException ex) {
            log("Topology file " + topologyFn + " does not exist", true);
            System.exit(-1);
        }

        try (Scanner sc = new Scanner(topologyFile)) {
            // add every node's neighbors
            String[] line = {};
            while (sc.hasNextLine()) {
                line = sc.nextLine().split(" ");
                curNode = line[0];

                for (int i = 1; i < line.length; i++) {
                    String nextNode = line[i];
                    Point3D point1 = locations.get(curNode);
                    Point3D point2 = locations.get(nextNode);
                    double distance = point1.distance(point2);
                    neighbors.get(curNode).addDestination(neighbors.get(line[i]), distance);
                }
            }
        } catch (FileNotFoundException ex) {
            log("Topology file " + topologyFn + " does not exist", true);
            System.exit(-1);
        }

        // add every node to the graph
        topology.setNodes(neighbors.values());
        if (debug)
            printTopology();

        createRoutingTable(topology);
    }

    public static long getCurTS() {
        return System.currentTimeMillis() - initTS;
    }

    private static boolean isLogger(LoggerStatus loggerStatus) {
        return Network.loggerStatus == loggerStatus;
    }

    private static boolean isLoggerActive() {
        return isLogger(LoggerStatus.ACTIVE);
    }

    public static boolean isLoggerExited() {
        return isLogger(LoggerStatus.EXIT);
    }

    public static void log(Object text, boolean debug) {
        if (!debug)
            return;
        if (text == null)
            text = "NULL";
        text = myID + ": " + text;
        long curTS = getCurTS();
        if (curTS >= 0)
            text += " at time " + curTS;
        System.out.println(text);
        synchronized (loggerLock) {
            if (isLoggerActive()) {
                try {
                    logger.write(text + "\n");
                    logger.flush();
                } catch (IOException e) {
                    System.out.println("Exception while logging!!!");
                }
            } else if (isLoggerExited()) {
                System.out.println(myID + ": Not logging anymore at time " + curTS);
            } else {
                System.out.println(myID + ": Not started logging yet at time " + curTS);
            }
        }
    }

    public static void log(Object text) {
        log(text, debug);
    }

    public static void flushLog() {
        log("Flushing and closing log");
        try {
            logger.flush();
            synchronized (loggerLock) {
                loggerStatus = LoggerStatus.EXIT;
                logger.close();
            }
            log("Flushed and closed log", getNodePort(myID) == 11111);
        } catch (IOException e) {
            log("Log already flushed and closed");
        }
    }

    public static String findMyIP() {
        try {
            for (final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces(); interfaces
                    .hasMoreElements();) {
                final NetworkInterface cur = interfaces.nextElement();

                if (cur.isLoopback()) {
                    continue;
                }

                for (final InterfaceAddress addr : cur.getInterfaceAddresses()) {
                    final InetAddress inet_addr = addr.getAddress();

                    if (!(inet_addr instanceof Inet4Address)) {
                        continue;
                    }
                    if (!inet_addr.getHostAddress().startsWith("192.168.4.")) {
                        continue;
                    }

                    myIP = inet_addr.getHostAddress();
                    return inet_addr.getHostAddress();
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getMyIP() {
        return myIP;
    }

    public static String setMyID(String nodeIP, String devVirtualID) {
        if (devVirtualID != null)
            myID = nodeIP + "_" + devVirtualID;
        else
            myID = nodeIP;
        return myID;
    }

    public static void setMyID(String nodeID) {
        myID = nodeID;
    }

    public static String getMyID() {
        return myID;
    }

    public static String getMyID(String devVirtualID) {
        if (devVirtualID != null)
            return getMyIP() + "_" + devVirtualID;
        else
            return getMyIP();
    }

    public static String getNodeIP(String nodeID) {
        return nodeID.split("_")[0];
    }

    public static int getNodePort(String nodeID) {
        return getNodePortVirt(nodeID.split("_")[1]);
    }

    public static int getNodePortVirt(String devVirtualID) {
        if (devVirtualID != null)
            return 11111 + Integer.parseInt(devVirtualID);
        else
            return 11111;
    }

    public static String getNodeID(String nodeIP, String devVirtualID) {
        if (devVirtualID != null)
            return nodeIP + "_" + devVirtualID;
        else
            return nodeIP;
    }

    public static List<String> getDevIDs() {
        return new ArrayList<>(locations.keySet());
    }

    public static boolean buildConnection(String nodeID, Message<? extends MessagePayload> msg) {
        boolean rv = false;
        try {
            // Create a client
            Client client = new Client(getNodeIP(nodeID), getNodePort(nodeID));
            ChannelFuture channelFuture = client.startup();

            // check the connection is successful
            if (channelFuture.isSuccess()) {
                Gson gson = new Gson();
                String jsonMsg = gson.toJson(msg);
                // send message to server
                channelFuture.channel().writeAndFlush(Unpooled.wrappedBuffer(jsonMsg.getBytes()))
                        .addListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture future) throws Exception {
                                if (!future.isSuccess())
                                    log("sending " + msg + " failed (1)", true);
                            }
                        });
            }
            // close the client
            client.shutdown();
            rv = true;
        } catch (Exception e) {
            log("sending " + msg + " failed (2)", true);
        }
        return rv;
    }

    public static void stopListening() {
        try {
            nettyServer.shutdown();
        } catch (Exception e) {
            log("Stopped listening to port " + myPort);
        }
    }

    public void printTopology() {
        log("printTopology:");
        for (String node : neighbors.keySet()) {
            Node value = neighbors.get(node);
            if (value != null) {
                System.out.println(node.toString() + ": " + value.toString());
            }
        }
    }

    public static Map<String, Membership> getMembershipList() {
        String devID;
        int curReinc, lastReinc;
        Membership curStatus, lastStatus;
        Pair<Membership, Integer> curDevEntry;
        Map<String, Pair<Membership, Integer>> current = new HashMap<>();
        Map<String, Membership> result = new HashMap<>();
        synchronized (memLock) {
            try (Scanner medleySc = new Scanner(new File(memTableFn))) {
                while (medleySc.hasNext()) {
                    devID = medleySc.next();
                    medleySc.skip(" --- ");
                    curReinc = medleySc.nextInt();
                    medleySc.skip(" --- ");
                    curStatus = Membership.valueOf(medleySc.next());
                    current.put(devID, new Pair<Membership, Integer>(curStatus, curReinc));
                    result.put(devID, curStatus);
                }
                for (Entry<String, Pair<Membership, Integer>> lastDevEntry : lastMembershipTable.entrySet()) {
                    devID = lastDevEntry.getKey();
                    curDevEntry = current.getOrDefault(devID,
                            new Pair<Membership, Integer>(Membership.ACTIVE, -1));
                    curStatus = curDevEntry.getKey();
                    curReinc = curDevEntry.getValue();
                    lastStatus = lastDevEntry.getValue().getKey();
                    lastReinc = lastDevEntry.getValue().getValue();
                    if (curStatus == Membership.FAILED && lastStatus != Membership.FAILED) {
                        current.put(devID, new Pair<Membership, Integer>(Membership.FAILED, lastReinc));
                        failedNodes.put(devID, new ArrayList<>(
                                neighbors.get(devID).getAdjacentNodesIDs()));
                    } else if (curStatus != Membership.FAILED && lastStatus == Membership.FAILED) {
                        if (curReinc > lastReinc) {
                            nodeJoined(devID);
                            failedNodes.remove(devID);
                        }
                    }
                    if (!result.containsKey(devID)) {
                        result.put(devID, Membership.FAILED);
                    }
                }
                lastMembershipTable.clear();
                lastMembershipTable.putAll(current);
            } catch (FileNotFoundException e) {
                log("Network: File " + memTableFn + " not found!");
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public Set<String> getNeighbors(String curNode) {
        Node node = neighbors.get(curNode);
        if (node != null) {
            return node.getAdjacentNodesIDs();
        } else {
            return null;
        }
    }

    public static void createRoutingTable(Graph topology) {
        Graph tempTopology, spanningTree;
        routingTable.clear();
        tempTopology = new Graph(topology);
        if (tempTopology.getNodeByID(myID) == null)
            return;
        if (debug)
            tempTopology.print();

        spanningTree = Dijkstra.calculateShortestPathFromSource(
                tempTopology, tempTopology.getNodeByID(myID));

        log("" + spanningTree);

        for (Node destNode : spanningTree.getNodes()) {
            if (destNode.getShortestPath().size() > 1) {
                routingTable.put(
                        destNode.getID(),
                        new Pair<String, Double>(
                                destNode.getShortestPath().get(1).getID(),
                                destNode.getDistance()));
            } else if (destNode.getShortestPath().size() == 1) {
                routingTable.put(
                        destNode.getID(), new Pair<String, Double>(
                                destNode.getID(), destNode.getDistance()));
            }
        }
        if (debug)
            printRoutingTable();
    }

    public static void printRoutingTable() {
        System.out.println("Routing table\nsource\tdestinations");
        System.out.print("\t");
        for (String node : neighbors.keySet()) {
            System.out.print(node + "\t");
        }

        System.out.print("\n" + myID);
        if (routingTable != null) {
            for (String dst : routingTable.keySet()) {
                System.out.print("\t");
                Pair<String, Double> neighbor = routingTable.get(dst);
                if (!myID.equals(dst) && neighbor != null)
                    System.out.print(neighbor.getKey());
            }
        }
        System.out.println();
    }

    public static Map<String, Pair<String, Double>> getRoutingTable() {
        return routingTable;
    }

    public static double getRouteOWD(String dst) {
        double routeOWD;
        if (myID.equals(dst)) {
            routeOWD = 0;
        } else if (getRoutingTable().get(dst) == null) {
            log(Arrays.toString(Thread.currentThread().getStackTrace()));
            log("from " + myID + " to " + dst + ": no routing entry?");
            routeOWD = 0;
        } else {
            routeOWD = getRoutingTable().get(dst).getValue();
        }
        return routeOWD;
    }

    public static double getRouteRTT(String dst) {
        return getRouteOWD(dst) * 2;
    }

    public static void nodeJoined(String newNodeID) {
        log("Node " + newNodeID + " joined");
        List<String> newNodeNeighbors = failedNodes.getOrDefault(newNodeID, new ArrayList<String>());
        neighbors.put(newNodeID, new Node(newNodeID));
        for (String nodeID : neighbors.keySet()) {
            if (newNodeNeighbors.contains(nodeID)) {
                Point3D point1 = locations.get(newNodeID);
                Point3D point2 = locations.get(nodeID);
                double distance = point1.distance(point2);
                neighbors.get(newNodeID).addDestination(neighbors.get(nodeID), distance);
                neighbors.get(nodeID).addDestination(neighbors.get(newNodeID), distance);
            }
        }

        Graph topology = new Graph();
        topology.setNodes(neighbors.values());
        createRoutingTable(topology);
    }

    public static void nodeFailed(String failedNodeID) {
        if (failedNodes == null) {
            log("!!!!!!!! failedNodes is NULL !!!!!!!!!");
        }
        if (failedNodeID == null) {
            log("!!!!!!!! failedNodeID is NULL !!!!!!!!!");
        }
        if (failedNodes.containsKey(failedNodeID))
            return;
        failedNodes.put(failedNodeID, new ArrayList<>(
                neighbors.get(failedNodeID).getAdjacentNodesIDs()));
        getMembershipList(); // to remove it from failedNodes if already known as failed
        Node failedNode = neighbors.get(failedNodeID);
        for (String nodeID : neighbors.keySet()) {
            if (neighbors.get(nodeID).getAdjacentNodesIDs().contains(failedNodeID)) {
                neighbors.get(nodeID).removeDestination(failedNode);
            }
        }
        neighbors.remove(failedNodeID);

        Graph topology = new Graph();
        topology.setNodes(neighbors.values());
        createRoutingTable(topology);
    }

    public static int getNextSeqNo() {
        int newSeqNo = -1;
        synchronized (seqNoLock) {
            newSeqNo = ++_seqNo;
        }
        return newSeqNo;
    }

    public static boolean sendMsg(Message<? extends MessagePayload> msg) {
        if (isLoggerExited()) {
            return false;
        }
        if (msg == null) {
            log("Msg is null");
            return false;
        }

        if (myID.equals(msg.getDstID())) {
            msgBuffer.add(msg);
        } else {
            while (true) {
                try {
                    String nextDevID = routingTable.get(msg.getDstID()).getKey();
                    log("msg: " + msg + ", next routing: " + nextDevID);
                    metric.recordH2HMsg(myID, nextDevID, msg.getType());
                    if (buildConnection(nextDevID, msg))
                        break;
                } catch (Exception e) {
                    log(e.getMessage());
                    log(msg.getDstID() + " is null?");
                    log("Sending " + msg
                            + " but routingTable.get(" + msg.getDstID() + ") is "
                            + routingTable.get(msg.getDstID()) + "\n" + routingTable);
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
            }
        }
        return true;
    }

    public static boolean existUnreadMsgs() {
        return !msgBuffer.isEmpty();
    }

    public static void addMsg(Message<? extends MessagePayload> msg) {
        metric.recordE2EMsg(msg.getSrcID(), msg.getDstID(), msg.getType());
        msgBuffer.add(msg);
    }

    public static Message<? extends MessagePayload> recvMsg() {
        return msgBuffer.remove();
    }

    public void clearMsgs() {
        msgBuffer.clear();
    }

    public static void setkMemberMix(KMemberMix kMemberMixV) {
        kMemberMix = kMemberMixV;
    }

    public static KMemberMix getkMemberMix() {
        return kMemberMix;
    }

    public int getNumberOfDevices() {
        return neighbors.size();
    }

}
