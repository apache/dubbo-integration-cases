/*
 *
 *   Licensed to the Apache Software Foundation (ASF) under one or more
 *   contributor license agreements.  See the NOTICE file distributed with
 *   this work for additional information regarding copyright ownership.
 *   The ASF licenses this file to You under the Apache License, Version 2.0
 *   (the "License"); you may not use this file except in compliance with
 *   the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.dubbo.samples.test;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.dubbo.samples.test.consumer.CustomFilter;
import org.junit.Assert;
import org.junit.Test;

public class ConsumerIT {

    @Test
    public void test() {
        ReferenceConfig<DemoService> reference1 = new ReferenceConfig<>();
        reference1.setInterface(DemoService.class);

        ReferenceConfig<CallTripleService> reference2 = new ReferenceConfig<>();
        reference2.setInterface(CallTripleService.class);

        ConsumerConfig consumerConfig = new ConsumerConfig();
        consumerConfig.setFilter("appended");

        DubboBootstrap.getInstance()
                .registry(new RegistryConfig("zookeeper://" + System.getProperty("zookeeper.address", "127.0.0.1") + ":2181"))
                .application(new ApplicationConfig("consumer"))
                .reference(reference1)
                .reference(reference2)
                .consumer(consumerConfig)
                .start();

        CustomFilter.resetExpected();
        DemoService demoService = reference1.get();
        demoService.sayHello(new User("dubbo"));

        String dubboExpectHost = CustomFilter.expectedHost();
        Assert.assertNotNull(dubboExpectHost);
        int dubboExpectedPort = CustomFilter.expectedPort();
        Assert.assertNotEquals(0, dubboExpectedPort);

        CustomFilter.resetExpected();
        CallTripleService callTripleService = reference2.get();
        callTripleService.sayHello(new User("triple"));

        String tripleExpectHost = CustomFilter.expectedHost();
        Assert.assertNotNull(tripleExpectHost);
        int tripleExpected = CustomFilter.expectedPort();
        Assert.assertNotEquals(0, tripleExpected);

    }

}
