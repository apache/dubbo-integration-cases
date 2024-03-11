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

package org.apache.dubbo.samples.provider;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.samples.api.GreetingsService;
import org.apache.dubbo.samples.api.QosService;

public class Application {

    public static void main(String[] args) {
        System.setProperty("dubbo.application.metadata.publish.delay", "1");
        ServiceConfig<GreetingsService> service = new ServiceConfig<>();
        service.setInterface(GreetingsService.class);
        service.setRef(new GreetingsServiceImpl());
        ServiceConfig<QosService> qosService = new ServiceConfig<>();
        qosService.setInterface(QosService.class);
        qosService.setRef(new QosServiceImpl());
        ApplicationConfig applicationConfig = new ApplicationConfig("first-dubbo-provider");
        applicationConfig.setRegisterMode("interface");

        DubboBootstrap dubboBootstrap = DubboBootstrap.getInstance()
                .application(applicationConfig)
                .registry(new RegistryConfig("N/A"))
                .protocol(new ProtocolConfig("tri", 50051))
                .service(service)
                .service(qosService)
                .start();

        System.out.println("dubbo service started");
        dubboBootstrap.await();
    }
}
