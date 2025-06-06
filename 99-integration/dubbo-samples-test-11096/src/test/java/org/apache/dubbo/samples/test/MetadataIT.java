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

import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.metadata.report.support.AbstractMetadataReport;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class MetadataIT {

    @Test
    public void test1() {
        ApplicationModel applicationModel = FrameworkModel.defaultModel().newApplication();

        ServiceConfig<DemoService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(DemoService.class);
        serviceConfig.setRef(new DemoServiceImpl());

        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setAddress("zookeeper://" + System.getProperty("zookeeper.address", "127.0.0.1") + ":2181");

        MetadataReportConfig metadataReportConfig = new MetadataReportConfig();
        metadataReportConfig.setCycleReport(false);

        DubboBootstrap dubboBootstrap = DubboBootstrap.getInstance(applicationModel);
        dubboBootstrap.application("metadata-test")
                .registry(registryConfig)
                .service(serviceConfig)
                .metadataReport(metadataReportConfig);
        dubboBootstrap.start();

        Assert.assertTrue(dubboBootstrap.isInitialized());
        Assert.assertTrue(dubboBootstrap.isStarted());

        MetadataReportInstance metadataReportInstance = applicationModel.getBeanFactory().getBean(MetadataReportInstance.class);
        Assert.assertNotNull(metadataReportInstance);
        Map<String, MetadataReport> metadataReports = metadataReportInstance.getMetadataReports(true);
        Assert.assertNotNull(metadataReports);
        Assert.assertEquals(1, metadataReports.size());
        FrameworkModel.destroyAll();

    }

    @Test
    public void test2() {
        ApplicationModel applicationModel = FrameworkModel.defaultModel().newApplication();

        ServiceConfig<DemoService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(DemoService.class);
        serviceConfig.setRef(new DemoServiceImpl());

        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setAddress("zookeeper://" + System.getProperty("zookeeper.address", "127.0.0.1") + ":2181");

        MetadataReportConfig metadataReportConfig = new MetadataReportConfig();
        metadataReportConfig.setRetryPeriod(1000);

        DubboBootstrap dubboBootstrap = DubboBootstrap.getInstance(applicationModel);
        dubboBootstrap.application("metadata-test")
                .registry(registryConfig)
                .service(serviceConfig)
                .metadataReport(metadataReportConfig);
        dubboBootstrap.start();

        Assert.assertTrue(dubboBootstrap.isInitialized());
        Assert.assertTrue(dubboBootstrap.isStarted());

        MetadataReportInstance metadataReportInstance = applicationModel.getBeanFactory().getBean(MetadataReportInstance.class);
        Assert.assertNotNull(metadataReportInstance);
        Map<String, MetadataReport> metadataReports = metadataReportInstance.getMetadataReports(true);
        Assert.assertNotNull(metadataReports);
        Assert.assertEquals(1, metadataReports.size());
        FrameworkModel.destroyAll();
    }

    @Test
    public void test3() {
        ApplicationModel applicationModel = FrameworkModel.defaultModel().newApplication();

        ServiceConfig<DemoService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(DemoService.class);
        serviceConfig.setRef(new DemoServiceImpl());

        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setAddress("zookeeper://" + System.getProperty("zookeeper.address", "127.0.0.1") + ":2181");
        registryConfig.setUseAsMetadataCenter(false);

        MetadataReportConfig metadataReportConfig = new MetadataReportConfig();
        metadataReportConfig.setRetryPeriod(1000);

        DubboBootstrap dubboBootstrap = DubboBootstrap.getInstance(applicationModel);
        dubboBootstrap.application("metadata-test")
                .registry(registryConfig)
                .service(serviceConfig)
                .metadataReport(metadataReportConfig);

        dubboBootstrap.start();

        Assert.assertTrue(dubboBootstrap.isInitialized());
        Assert.assertTrue(dubboBootstrap.isStarted());

        MetadataReportInstance metadataReportInstance = applicationModel.getBeanFactory().getBean(MetadataReportInstance.class);
        Assert.assertNotNull(metadataReportInstance);
        Map<String, MetadataReport> metadataReports = metadataReportInstance.getMetadataReports(true);
        Assert.assertNotNull(metadataReports);
        Assert.assertEquals(0, metadataReports.size());
        FrameworkModel.destroyAll();
    }

    @Test
    public void test4() {
        ApplicationModel applicationModel = FrameworkModel.defaultModel().newApplication();

        ServiceConfig<DemoService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(DemoService.class);
        serviceConfig.setRef(new DemoServiceImpl());

        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setAddress("zookeeper://" + System.getProperty("zookeeper.address", "127.0.0.1") + ":2181");
        registryConfig.setUseAsMetadataCenter(false);

        MetadataReportConfig metadataReportConfig = new MetadataReportConfig();
        metadataReportConfig.setAddress("10.0.0.0");

        DubboBootstrap dubboBootstrap = DubboBootstrap.getInstance(applicationModel);
        dubboBootstrap.application("metadata-test")
                .registry(registryConfig)
                .service(serviceConfig)
                .metadataReport(metadataReportConfig);

        try {
            dubboBootstrap.start();
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("Please specify valid protocol or address for metadata report"));
        }

        FrameworkModel.destroyAll();
    }

    @Test
    public void test5() {
        ApplicationModel applicationModel = FrameworkModel.defaultModel().newApplication();

        ServiceConfig<DemoService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(DemoService.class);
        serviceConfig.setRef(new DemoServiceImpl());

        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setAddress("zookeeper://" + System.getProperty("zookeeper.address", "127.0.0.1") + ":2181");
        registryConfig.setUseAsMetadataCenter(false);

        MetadataReportConfig metadataReportConfig = new MetadataReportConfig();
        metadataReportConfig.setAddress("10.0.0.0");
        metadataReportConfig.setReportMetadata(false);
        metadataReportConfig.setReportDefinition(false);

        DubboBootstrap dubboBootstrap = DubboBootstrap.getInstance(applicationModel);
        dubboBootstrap.application("metadata-test")
                .registry(registryConfig)
                .service(serviceConfig)
                .metadataReport(metadataReportConfig);

        dubboBootstrap.start();

        Assert.assertTrue(dubboBootstrap.isInitialized());
        Assert.assertTrue(dubboBootstrap.isStarted());

        MetadataReportInstance metadataReportInstance = applicationModel.getBeanFactory().getBean(MetadataReportInstance.class);
        Assert.assertNotNull(metadataReportInstance);
        Map<String, MetadataReport> metadataReports = metadataReportInstance.getMetadataReports(true);
        Assert.assertNotNull(metadataReports);
        Assert.assertEquals(0, metadataReports.size());
        FrameworkModel.destroyAll();
    }

    @Test
    public void test6() {
        ApplicationModel applicationModel = FrameworkModel.defaultModel().newApplication();

        ServiceConfig<DemoService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(DemoService.class);
        serviceConfig.setRef(new DemoServiceImpl());

        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setAddress("zookeeper://" + System.getProperty("zookeeper.address", "127.0.0.1") + ":2181");

        MetadataReportConfig metadataReportConfig = new MetadataReportConfig();
        metadataReportConfig.setCycleReport(false);

        DubboBootstrap dubboBootstrap = DubboBootstrap.getInstance(applicationModel);
        dubboBootstrap.application("metadata-test")
                .registry(registryConfig)
                .service(serviceConfig)
                .metadataReport(metadataReportConfig);

        dubboBootstrap.start();

        Assert.assertTrue(dubboBootstrap.isInitialized());
        Assert.assertTrue(dubboBootstrap.isStarted());

        MetadataReportInstance metadataReportInstance = applicationModel.getBeanFactory().getBean(MetadataReportInstance.class);
        Assert.assertNotNull(metadataReportInstance);
        Map<String, MetadataReport> metadataReports = metadataReportInstance.getMetadataReports(true);
        Assert.assertNotNull(metadataReports);
        Assert.assertEquals(1, metadataReports.size());
        AbstractMetadataReport report = (AbstractMetadataReport) metadataReports.entrySet().iterator().next().getValue();
        Assert.assertFalse(report.getUrl().getParameter("cycle-report", true));
        FrameworkModel.destroyAll();
    }

    @Test
    public void test7() {
        ApplicationModel applicationModel = FrameworkModel.defaultModel().newApplication();

        ServiceConfig<DemoService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(DemoService.class);
        serviceConfig.setRef(new DemoServiceImpl());

        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setAddress("zookeeper://" + System.getProperty("zookeeper.address", "127.0.0.1") + ":2181");

        MetadataReportConfig metadataReportConfig = new MetadataReportConfig();
        metadataReportConfig.setReportMetadata(false);

        DubboBootstrap dubboBootstrap = DubboBootstrap.getInstance(applicationModel);
        dubboBootstrap.application("metadata-test")
                .registry(registryConfig)
                .service(serviceConfig)
                .metadataReport(metadataReportConfig);

        dubboBootstrap.start();

        Assert.assertTrue(dubboBootstrap.isInitialized());
        Assert.assertTrue(dubboBootstrap.isStarted());

        MetadataReportInstance metadataReportInstance = applicationModel.getBeanFactory().getBean(MetadataReportInstance.class);
        Assert.assertNotNull(metadataReportInstance);
        Map<String, MetadataReport> metadataReports = metadataReportInstance.getMetadataReports(true);
        Assert.assertNotNull(metadataReports);
        Assert.assertEquals(1, metadataReports.size());
        AbstractMetadataReport report = (AbstractMetadataReport) metadataReports.entrySet().iterator().next().getValue();
        Assert.assertFalse(report.shouldReportMetadata());
        FrameworkModel.destroyAll();
    }
}
