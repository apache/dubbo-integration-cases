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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpScheme;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.SslConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.samples.test.netty.Http2ClientInitializer;
import org.apache.dubbo.samples.test.netty.Http2SettingsHandler;
import org.apache.dubbo.samples.test.netty.HttpResponseHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Establishing an HTTP/2 connection via ALPN or h2c.
 */
public final class Http2ClientT {

    public static final Logger LOGGER = LoggerFactory.getLogger(Http2ClientT.class);

    static final String HOST = System.getProperty("host", "127.0.0.1");

    static final int PORT = Integer.parseInt(System.getProperty("port", "50052"));

    static final String URL2 = System.getProperty("url2", "/org.apache.dubbo.samples.test.DemoService/sayHello");

    static final String URL2DATA = System.getProperty("url2data", "[\"World\"]");

    static ClassLoader cl = Http2ClientT.class.getClassLoader();

    static DubboBootstrap bootstrap;

    static EmbeddedZooKeeper zooKeeper = new EmbeddedZooKeeper(2181, false);

    @BeforeAll
    static void initServer() {
        zooKeeper.start();

        SslConfig sslConfig = new SslConfig();
        sslConfig.setServerKeyCertChainPath("classpath:certs/server0.pem");
        sslConfig.setServerPrivateKeyPath("classpath:certs/server0.key");
        sslConfig.setServerTrustCertCollectionPath("classpath:certs/ca.pem");

        ProtocolConfig protocolConfig = new ProtocolConfig("tri");
        protocolConfig.setSslEnabled(true);
        protocolConfig.setPort(PORT);

        bootstrap = DubboBootstrap.getInstance();
        bootstrap.application(new ApplicationConfig("ssl-provider"))
                .registry(new RegistryConfig("zookeeper://" + HOST + ":2181"))
                .protocol(protocolConfig)
                .ssl(sslConfig);
        ServiceConfig<DemoService> service = new ServiceConfig<>();
        service.setInterface(DemoService.class);
        service.setRef(new DemoServiceImpl());

        bootstrap.service(service);
        bootstrap.start();
    }

    @AfterAll
    static void stopServer() {
        bootstrap.stop();
        zooKeeper.stop();
    }

    @Test
    void testH2c() throws Exception {
        Http2ClientInitializer initializer = new Http2ClientInitializer(null, Integer.MAX_VALUE);
        testHttp2(initializer);
    }

    @Test
    void testALPN() throws Exception {
        SslProvider provider = OpenSsl.isAlpnSupported() ? SslProvider.OPENSSL : SslProvider.JDK;
        SslContext sslCtx = SslContextBuilder.forClient()
                .sslProvider(provider)
                /* NOTE: the cipher filter may not include all ciphers required by the HTTP/2 specification.
                 * Please refer to the HTTP/2 specification for cipher requirements. */
                .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .applicationProtocolConfig(new ApplicationProtocolConfig(
                        Protocol.ALPN,
                        // NO_ADVERTISE is currently the only mode supported by both OpenSsl and JDK providers.
                        SelectorFailureBehavior.NO_ADVERTISE,
                        // ACCEPT is currently the only mode supported by both OpenSsl and JDK providers.
                        SelectedListenerFailureBehavior.ACCEPT,
                        ApplicationProtocolNames.HTTP_2,
                        ApplicationProtocolNames.HTTP_1_1))
                .keyManager(cl.getResourceAsStream("certs/client.pem"), cl.getResourceAsStream("certs/client.key"))
                .trustManager(cl.getResourceAsStream("certs/ca.pem")
                )
                .build();

        Http2ClientInitializer initializer = new Http2ClientInitializer(sslCtx, Integer.MAX_VALUE);
        testHttp2(initializer);
    }


    private void testHttp2(Http2ClientInitializer initializer) throws Exception {
        EventLoopGroup workerGroup = new NioEventLoopGroup(4);

        String result;
        try {
            // Configure the client.
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.remoteAddress(HOST, PORT);
            b.handler(initializer);

            // Start the client.
            Channel channel = b.connect().syncUninterruptibly().channel();
            LOGGER.info("Connected to [" + HOST + ':' + PORT + ']');

            // Wait for the HTTP/2 upgrade to occur.
            Http2SettingsHandler http2SettingsHandler = initializer.settingsHandler();
            http2SettingsHandler.awaitSettings(5, TimeUnit.MINUTES);

            HttpResponseHandler responseHandler = initializer.responseHandler();
            int streamId = 3;
            HttpScheme scheme = HttpScheme.HTTP;
            AsciiString hostName = new AsciiString(HOST + ':' + PORT);

            LOGGER.info("Sending request(s)...");
            // Create a simple POST request with a body.
            FullHttpRequest request = new DefaultFullHttpRequest(HTTP_1_1, POST, URL2,
                    wrappedBuffer(URL2DATA.getBytes(CharsetUtil.UTF_8)));
            request.headers().add(HttpHeaderNames.HOST, hostName);
            request.headers().add(HttpConversionUtil.ExtensionHeaderNames.SCHEME.text(), scheme.name());
            request.headers().add(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
            request.headers().add(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.DEFLATE);
            request.headers().add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
            responseHandler.put(streamId, channel.write(request), channel.newPromise());

            channel.flush();
            responseHandler.awaitResponses(5, TimeUnit.SECONDS);
            LOGGER.info("Finished HTTP/2 request(s)");
            result = responseHandler.getLastResult();

            // Wait until the connection is closed.
            channel.close().syncUninterruptibly();
        } finally {
            workerGroup.shutdownGracefully();
        }
        Assertions.assertEquals("\"Hello World\"", result);
    }
}
