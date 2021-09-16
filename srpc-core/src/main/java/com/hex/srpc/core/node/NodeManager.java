package com.hex.srpc.core.node;

import com.google.common.collect.Sets;
import com.hex.common.constant.LoadBalanceRule;
import com.hex.common.exception.RpcException;
import com.hex.common.net.HostAndPort;
import com.hex.srpc.core.connection.ConnectionPool;
import com.hex.srpc.core.connection.IConnection;
import com.hex.srpc.core.connection.IConnectionPool;
import com.hex.srpc.core.loadbalance.LoadBalancerFactory;
import com.hex.srpc.core.loadbalance.LoadBalancer;
import com.hex.srpc.core.rpc.Client;
import com.hex.srpc.core.rpc.client.SRpcClient;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author: hs
 * 维护各个server连接
 */
public class NodeManager implements INodeManager {
    private static final Logger logger = LoggerFactory.getLogger(NodeManager.class);

    private boolean isClient;
    private final Set<HostAndPort> servers = Sets.newConcurrentHashSet();
    private final Map<HostAndPort, IConnectionPool> connectionPoolMap = new ConcurrentHashMap<>();
    private static final Map<HostAndPort, NodeStatus> nodeStatusMap = new ConcurrentHashMap<>();
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private SRpcClient client;
    private int poolSizePerServer;

    public NodeManager(boolean isClient) {
        this.isClient = isClient;
    }

    public NodeManager(boolean isClient, SRpcClient client, int poolSizePerServer) {
        this.isClient = isClient;
        this.client = client;
        this.poolSizePerServer = poolSizePerServer;
    }

    @Override
    public synchronized void addNode(HostAndPort node) {
        if (isClosed.get()) {
            logger.error("nodeManager closed, add server {} failed", node);
        }
        if (!servers.contains(node)) {
            servers.add(node);
            IConnectionPool connectionPool = connectionPoolMap.get(node);
            if (connectionPool != null) {
                connectionPool.close();
            }
            connectionPoolMap.put(node, new ConnectionPool(poolSizePerServer, node, client));
        }
    }

    @Override
    public void addNodes(List<HostAndPort> servers) {
        for (HostAndPort server : servers) {
            addNode(server);
        }
    }

    @Override
    public synchronized void removeNode(HostAndPort server) {
        servers.remove(server);
        IConnectionPool connectionPool = connectionPoolMap.get(server);
        if (connectionPool != null) {
            connectionPool.close();
            connectionPoolMap.remove(server);
        }
        NodeStatus nodeStatus = nodeStatusMap.get(server);
        if (nodeStatus != null) {
            nodeStatusMap.remove(server);
        }
    }

    @Override
    public HostAndPort[] getAllRemoteNodes() {
        return servers.toArray(new HostAndPort[]{});
    }

    @Override
    public int getNodesSize() {
        return servers.size();
    }

    @Override
    public IConnectionPool getConnectionPool(HostAndPort node) {
        return connectionPoolMap.get(node);
    }

    @Override
    public Map<String, AtomicInteger> getConnectionSize() {
        if (CollectionUtils.isEmpty(servers)) {
            return Collections.emptyMap();
        }
        Map<String, AtomicInteger> connectionSizeMap = new HashMap<>();
        for (HostAndPort server : servers) {
            AtomicInteger counter = connectionSizeMap.computeIfAbsent(server.getHost(),
                    k -> new AtomicInteger(0));
            counter.addAndGet(connectionPoolMap.get(server).size());
        }
        return connectionSizeMap;
    }

    @Override
    public List<HostAndPort> chooseHANode(List<HostAndPort> nodes) {
        if (nodes.size() == 1) {
            return nodes;
        }
        // 过滤出可用的server
        List<HostAndPort> availableServers = nodes.stream()
                .filter(server -> nodeStatusMap.get(server) == null || nodeStatusMap.get(server).isAvailable())
                .collect(Collectors.toList());
        if (availableServers.isEmpty()) {
            throw new RpcException("no available server");
        }
        return availableServers;
    }

    @Override
    public void closeManager() {
        if (isClosed.compareAndSet(false, true)) {
            for (IConnectionPool connectionPool : connectionPoolMap.values()) {
                connectionPool.close();
            }
            servers.clear();
            connectionPoolMap.clear();
        }
    }

    public IConnection getConnectionFromPool(HostAndPort address) {
        IConnectionPool connectionPool = connectionPoolMap.get(address);
        if (connectionPool == null) {
            addNode(address);
            connectionPool = connectionPoolMap.get(address);
        }
        return connectionPool.getConnection();
    }

    @Override
    public Map<HostAndPort, NodeStatus> getNodeStatusMap() {
        return nodeStatusMap;
    }

    @Override
    public Client getClient() {
        return client;
    }

    public static void serverError(HostAndPort server) {
        NodeStatus nodeStatus = nodeStatusMap.get(server);
        if (nodeStatus == null) {
            synchronized (nodeStatusMap) {
                if ((nodeStatus = nodeStatusMap.get(server)) == null) {
                    nodeStatus = new NodeStatus(server);
                    nodeStatusMap.put(server, nodeStatus);
                }
            }
        }
        nodeStatus.errorTimesInc();
    }
}