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
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.metrics.collector.CombMetricsCollector;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.data.BaseStatComposite;
import org.apache.dubbo.metrics.data.RtStatComposite;
import org.apache.dubbo.metrics.model.container.LongContainer;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.samples.api.GreetingsService;

import cn.hutool.http.HttpUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class GreetingServiceIT {
    private static String zookeeperHost = System.getProperty("zookeeper.address", "127.0.0.1");

    @Test
    public void test1() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        ApplicationConfig applicationConfig = new ApplicationConfig("first-dubbo-consumer");
        applicationConfig.setQosPort(22333);
        ReferenceConfig<GreetingsService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterface(GreetingsService.class);

        MetricsConfig metricsConfig = new MetricsConfig();
        metricsConfig.setEnableRpc(true);

        DubboBootstrap.getInstance()
                .application(applicationConfig)
                .metrics(metricsConfig)
                .registry(new RegistryConfig("zookeeper://" + zookeeperHost + ":2181"))
                .reference(referenceConfig)
                .start();

        GreetingsService greetingsService = referenceConfig.get();

        String message = greetingsService.echo1("hello");
        Assertions.assertEquals("hello", message);

        message = greetingsService.echo2("hello");
        Assertions.assertEquals("hello", message);

        for (int i = 0; i < 10; i++) {
            String result = HttpUtil.get("http://127.0.0.1:22333/metrics");
            Map<String, String> data = new HashMap<>();
            for (String line : result.split("\n")) {
                if (line.startsWith("dubbo_consumer_rt_milliseconds_avg")) {
                    if (line.contains("echo1")) {
                        data.put("echo1", line.split(" ")[1]);
                    } else if (line.contains("echo2")) {
                        data.put("echo2", line.split(" ")[1]);
                    }
                }
            }
            Assertions.assertEquals(2, data.size());
            Assertions.assertTrue(Double.parseDouble(data.get("echo1")) > 0);
            Assertions.assertTrue(Double.parseDouble(data.get("echo2")) > 0);
        }

        DefaultMetricsCollector metricsCollector = FrameworkModel.defaultModel().defaultApplication().getBeanFactory().getBean(DefaultMetricsCollector.class);
        Method getStatsMethod = CombMetricsCollector.class.getDeclaredMethod("getStats");
        getStatsMethod.setAccessible(true);
        BaseStatComposite stats = (BaseStatComposite) getStatsMethod.invoke(metricsCollector);
        Field rtStatCompositeField = BaseStatComposite.class.getDeclaredField("rtStatComposite");
        rtStatCompositeField.setAccessible(true);
        RtStatComposite rtStatComposite = (RtStatComposite) rtStatCompositeField.get(stats);
        Field rtStatsField = RtStatComposite.class.getDeclaredField("rtStats");
        rtStatsField.setAccessible(true);
        Map<String, List<LongContainer<? extends Number>>> rtStats = (Map) rtStatsField.get(rtStatComposite);
        List<LongContainer<? extends Number>> longContainers = rtStats.get("consumer");
        LongContainer<? extends Number> longContainer = longContainers.get(4);
        for (Number value : longContainer.values()) {
            ((AtomicLong) value).set(0);
        }

        for (int i = 0; i < 10; i++) {
            String result = HttpUtil.get("http://127.0.0.1:22333/metrics");
            Map<String, String> data = new HashMap<>();
            for (String line : result.split("\n")) {
                if (line.startsWith("dubbo_consumer_rt_milliseconds_avg")) {
                    if (line.contains("echo1")) {
                        data.put("echo1", line.split(" ")[1]);
                    } else if (line.contains("echo2")) {
                        data.put("echo2", line.split(" ")[1]);
                    }
                }
            }
            Assertions.assertEquals(2, data.size());
            Assertions.assertEquals(0, Double.parseDouble(data.get("echo1")));
            Assertions.assertEquals(0, Double.parseDouble(data.get("echo2")));
        }

        FrameworkModel.destroyAll();
    }

}
