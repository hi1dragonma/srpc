package com.hex.srpc.core.rpc;

import java.util.List;
import java.util.Set;

/**
 * @author hs
 */
public interface Server {

    /**
     * 包含@RouteScan注解的类
     *
     * @param clazz
     * @return
     */
    Server sourceClass(Class<?> clazz);

    /**
     * 手动添加扫描包路径[可选]
     */
    Server configScanPackages(Set<String> packages);

    /**
     * 设置注册中心地址
     *
     * @param schema          注册中心模式[默认zookeeper]
     * @param registryAddress 注册中心地址
     * @param serviceName     需要发布到注册中心上的服务名称[服务提供端需要配置]
     * @return
     */
    Server configRegistry(String schema, List<String> registryAddress, String serviceName);

    /**
     * 指定端口,未指定的话则使用ServerConfig配置里的端口
     *
     * @param port 端口
     * @return
     */
    Server port(int port);

    /**
     * 启动服务端
     */
    Server start();

    /**
     * 停止服务端
     */
    void stop();
}