package com.hex.rpc.sping.registry;

import com.hex.common.net.HostAndPort;
import com.hex.rpc.sping.annotation.SRpcClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.type.AnnotationMetadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * @author: hs
 */
public class RpcServerAddressRegistry {
    private static final Logger logger = LoggerFactory.getLogger(RpcServerAddressRegistry.class);

    private static final Map<String, List<HostAndPort>> CLASS_ADDRESS_MAP = new ConcurrentHashMap<>();
    private static final Map<String, String> CLASS_SERVICE_NAME_MAP = new ConcurrentHashMap<>();

    public static void register(AnnotationMetadata annotationMetadata) {
        Class<?> clazz;
        try {
            clazz = Class.forName(annotationMetadata.getClassName());
        } catch (ClassNotFoundException e) {
            logger.error("load client Class failed", e);
            return;
        }
        if (!clazz.isAnnotationPresent(SRpcClient.class)) {
            logger.error("Class {} no @RpcClient annotation, register rpc server address failed", clazz);
            return;
        }
        SRpcClient annotation = clazz.getAnnotation(SRpcClient.class);
        String[] nodes = annotation.nodes();
        String serviceName = annotation.serviceName();
        if (nodes.length == 0 && StringUtils.isBlank(serviceName)) {
            logger.error("annotation @RpcClient must define attribute nodes or serviceName, Class {}", clazz);
        }
        if (nodes.length != 0) {
            List<HostAndPort> hostAndPorts = CLASS_ADDRESS_MAP.computeIfAbsent(clazz.getCanonicalName(),
                    k -> new CopyOnWriteArrayList<>());
            for (String node : nodes) {
                hostAndPorts.add(HostAndPort.from(node));
            }
        }
        if (StringUtils.isNotBlank(serviceName)) {
            CLASS_SERVICE_NAME_MAP.put(clazz.getCanonicalName(), serviceName);
        }
    }

    public static String getServiceName(String className) {
        return CLASS_SERVICE_NAME_MAP.get(className);
    }

    public static List<HostAndPort> getHostAndPorts(String className) {
        return CLASS_ADDRESS_MAP.get(className);
    }

    public static List<HostAndPort> getAll() {
        return CLASS_ADDRESS_MAP.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

}
