package com.hex.netty.loadbalance;

import com.hex.netty.constant.LoadBalanceRule;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author guohs
 * @date 2021/7/15
 */
public class RoundLoadBalance implements LoadBalance {
    @Override
    public LoadBalanceRule getRule() {
        return null;
    }

    @Override
    public InetSocketAddress getServer(List<InetSocketAddress> servers) {
        return null;
    }
}