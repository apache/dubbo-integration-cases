/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.samples.test;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.SslConfig;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Date;

public class DubboConsumerT {

    static EmbeddedZooKeeper zooKeeper = new EmbeddedZooKeeper(2181, false);

    static final String HOST = System.getProperty("host", "127.0.0.1");

    static final int PORT = Integer.parseInt(System.getProperty("port", "50052"));

    static final String zkAddress = "zookeeper://" + HOST + ":2181";

    DubboBootstrap providerBootstrap;

    DubboBootstrap consumerBootstrap;

    DemoService demoService;

    @BeforeAll
    static void initServer() {
        // for mac
        System.setProperty("dubbo.network.interface.preferred", "en0");
        zooKeeper.start();
    }

    @AfterAll
    static void stopServer() {
        zooKeeper.stop();
    }

    @AfterEach
    void stopService() {
        if (providerBootstrap != null) {
            providerBootstrap.stop();
        }
        if (consumerBootstrap != null) {
            consumerBootstrap.stop();
        }
    }

    @Test
    public void testTripleRpcWithSsl() {
        startProvider("tri", true);
        startConsumer("tri", true);
        Assertions.assertEquals("Hello World", demoService.sayHello("World"));
    }

    @Test
    public void testTripleRpc() {
        startProvider("tri", false);
        startConsumer("tri", false);
        Assertions.assertEquals("Hello World", demoService.sayHello("World"));
    }

    @Test
    public void testDubboRpcWithSsl() {
        startProvider("dubbo", true);
        startConsumer("dubbo", true);
        Assertions.assertEquals("Hello World", demoService.sayHello("World"));
    }

    @Test
    public void testDubboRpc() {
        startProvider("dubbo", false);
        startConsumer("dubbo", false);
        Assertions.assertEquals("Hello World", demoService.sayHello("World"));
    }


    private void startProvider(String protocol, boolean sslEnabled) {
        ProtocolConfig protocolConfig = new ProtocolConfig(protocol);
        protocolConfig.setSslEnabled(sslEnabled);
        protocolConfig.setPort(PORT);

        ApplicationConfig applicationConfig = new ApplicationConfig("dubbo-provider");
        applicationConfig.setQosPort(22222);

        DubboBootstrap bootstrap = DubboBootstrap.newInstance();
        bootstrap.application(applicationConfig)
                .registry(new RegistryConfig(zkAddress))
                .protocol(protocolConfig);
        if (sslEnabled) {
            SslConfig sslConfig = new SslConfig();
            sslConfig.setServerKeyCertChainPath("classpath:certs/server0.pem");
            sslConfig.setServerPrivateKeyPath("classpath:certs/server0.key");
            sslConfig.setServerTrustCertCollectionPath("classpath:certs/ca.pem");
            bootstrap.ssl(sslConfig);
        }

        ServiceConfig<DemoService> service = new ServiceConfig<>();
        service.setInterface(DemoService.class);
        service.setRef(new DemoServiceImpl());

        bootstrap.service(service);
        bootstrap.start();
        providerBootstrap = bootstrap;
    }

    public void startConsumer(String protocol, boolean sslEnabled) {
        ReferenceConfig<DemoService> reference1 = new ReferenceConfig<>();
        reference1.setInterface(DemoService.class);
        reference1.setProtocol(protocol);
        reference1.setCheck(false);

        ApplicationConfig applicationConfig = new ApplicationConfig("dubbo-consumer");
        applicationConfig.setQosPort(33333);

        DubboBootstrap bootstrap = DubboBootstrap.newInstance();
        bootstrap.application(applicationConfig)
                .registry(new RegistryConfig(zkAddress));
        if (sslEnabled) {
            SslConfig sslConfig = new SslConfig();
            sslConfig.setClientTrustCertCollectionPath("classpath:certs/ca.pem");
            sslConfig.setClientKeyCertChainPath("classpath:certs/client.pem");
            sslConfig.setClientPrivateKeyPath("classpath:certs/client.key");
            bootstrap.ssl(sslConfig);
        }
        bootstrap.reference(reference1);
        bootstrap.start();
        consumerBootstrap = bootstrap;

        demoService = reference1.get();
    }

}
