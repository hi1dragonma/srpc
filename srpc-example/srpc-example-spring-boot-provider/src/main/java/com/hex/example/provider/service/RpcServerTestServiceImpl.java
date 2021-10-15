package com.hex.example.provider.service;

import com.hex.common.annotation.Mapping;
import com.hex.common.annotation.SRpcRoute;
import com.hex.example.entity.TestRequest;
import com.hex.example.entity.TestResponse;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author: hs
 * <p>
 * SprcRoute服务1
 * 标注了@SRpcRoute注解后类似@Service，可使用spring相关注解@Autowired等
 */
@SRpcRoute
public class RpcServerTestServiceImpl {

    @Autowired
    private TestService testService;

    @Mapping("test1")
    public String handler(String body) {

        System.out.println("test1收到请求内容：" + body);

        return "这是test1响应内容";
    }

    @Mapping("test2")
    public TestResponse handler2(TestRequest request) {

        System.out.println(testService.get());

        System.out.println("test2收到请求内容:" + request);

        return new TestResponse().setResponse("test2响应结果");
    }

    @Mapping("test3")
    public void handler3() {

        System.out.println("test3收到请求");

    }
}