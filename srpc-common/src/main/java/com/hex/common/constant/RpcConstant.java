package com.hex.common.constant;

/**
 * @author: hs
 */
public interface RpcConstant {

    /**
     * 默认线程数 = cpu核数 * 2 +1
     */
    int DEFAULT_THREADS = Runtime.getRuntime().availableProcessors() * 2 + 1;

    String PING = "ping";

    String PONG = "pong";

    String CUSTOM_DUPLICATE_MARKER = "customDuplicatedMarker";
}