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

package org.apache.dubbo.samples.client;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.samples.api.GreetingsService;
import org.apache.dubbo.samples.api.QosService;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class GreetingServiceIT {
    private static String zookeeperHost = System.getProperty("zookeeper.address", "127.0.0.1");

    private final static long ACCEPTABLE_ERROR = 200;

    private final static ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

    @Test
    void test() {
        ReferenceConfig<QosService> qosReference = new ReferenceConfig<>();
        qosReference.setInterface(QosService.class);
        qosReference.setUrl("tri://" + System.getProperty("provider_address", "127.0.0.1") + ":50051?serialization=fastjson2&timeout=600000");

        DubboBootstrap.getInstance()
                .application(new ApplicationConfig("first-dubbo-consumer"))
                .registry(new RegistryConfig("N/A"))
                .reference(qosReference)
                .start();

        QosService qosService = qosReference.get();

        for (int i = 0; i < 20; i++) {
            bench(50);
        }
        for (int i = 0; i < 5; i++) {
            bench(10);
            qosService.usedMemory();
        }

        List<BigDecimal> memory = new ArrayList<>();
        memory.add(BigDecimal.valueOf(qosService.usedMemory()));

        for (int i = 0; i < 10; i++) {
            bench(50);
            memory.add(BigDecimal.valueOf(qosService.usedMemory()));
            System.out.println(new Date() + " memory: " + memory.get(i) + " index: " + i);
        }

        long avg = memory.stream().reduce(BigDecimal::add).get().divide(BigDecimal.valueOf(memory.size()), RoundingMode.DOWN).longValue();
        for (int i = 0; i < memory.size(); i++) {
            if (Math.abs(memory.get(i).longValue() - avg) > ACCEPTABLE_ERROR) {
                Assertions.fail("memory leak, avg: " + avg + ", current: " + memory.get(i) + ", index: " + i);
            }
        }
        System.out.println("avg: " + avg);
    }

    private static void bench(int range) {
        List<Future<?>> futures = new ArrayList<>();
        for (int j = 0; j < range; j++) {
            futures.add(EXECUTOR_SERVICE.submit(() -> {
                FrameworkModel frameworkModel = new FrameworkModel();
                ApplicationModel applicationModel = frameworkModel.newApplication();
                ReferenceConfig<GreetingsService> reference = new ReferenceConfig<>();
                reference.setInterface(GreetingsService.class);
                reference.setUrl("tri://" + System.getProperty("provider_address", "127.0.0.1") + ":50051?serialization=fastjson2");

                ApplicationConfig applicationConfig = new ApplicationConfig("first-dubbo-consumer");
                applicationConfig.setRegisterMode("interface");
                DubboBootstrap.getInstance(applicationModel)
                        .application(applicationConfig)
                        .registry(new RegistryConfig("N/A"))
                        .reference(reference)
                        .start();

                GreetingsService service = reference.get();
                for (int i = 0; i < 1000; i++) {
                    service.sayHi("dubbo");
                }
                frameworkModel.destroy();
            }));
        }
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
