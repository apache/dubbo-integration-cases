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
package org.apache.dubbo.samples.test.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap.SimpleEntry;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Process {@link FullHttpResponse} translated from HTTP/2 frames
 */
public class HttpResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    public static final Logger LOGGER = LoggerFactory.getLogger(HttpResponseHandler.class);

    private final Map<Integer, Entry<ChannelFuture, ChannelPromise>> streamidPromiseMap;

    private String lastResult;

    public HttpResponseHandler() {
        // Use a concurrent map because we add and iterate from the main thread (just for the purposes of the example),
        // but Netty also does a get on the map when messages are received in a EventLoop thread.
        streamidPromiseMap = new ConcurrentHashMap<>();
    }

    public String getLastResult() {
        return lastResult;
    }

    /**
     * Create an association between an anticipated response stream id and a {@link ChannelPromise}
     *
     * @param streamId The stream for which a response is expected
     * @param writeFuture A future that represent the request write operation
     * @param promise The promise object that will be used to wait/notify events
     * @return The previous object associated with {@code streamId}
     * @see HttpResponseHandler#awaitResponses(long, TimeUnit)
     */
    public Entry<ChannelFuture, ChannelPromise> put(int streamId, ChannelFuture writeFuture, ChannelPromise promise) {
        return streamidPromiseMap.put(streamId, new SimpleEntry<>(writeFuture, promise));
    }

    /**
     * Wait (sequentially) for a time duration for each anticipated response
     *
     * @param timeout Value of time to wait for each response
     * @param unit Units associated with {@code timeout}
     * @see HttpResponseHandler#put(int, ChannelFuture, ChannelPromise)
     */
    public void awaitResponses(long timeout, TimeUnit unit) {
        Iterator<Entry<Integer, Entry<ChannelFuture, ChannelPromise>>> itr = streamidPromiseMap.entrySet().iterator();
        while (itr.hasNext()) {
            Entry<Integer, Entry<ChannelFuture, ChannelPromise>> entry = itr.next();
            ChannelFuture writeFuture = entry.getValue().getKey();
            if (!writeFuture.awaitUninterruptibly(timeout, unit)) {
                throw new IllegalStateException("Timed out waiting to write for stream id " + entry.getKey());
            }
            if (!writeFuture.isSuccess()) {
                throw new RuntimeException(writeFuture.cause());
            }
            ChannelPromise promise = entry.getValue().getValue();
            if (!promise.awaitUninterruptibly(timeout, unit)) {
                throw new IllegalStateException("Timed out waiting for response on stream id " + entry.getKey());
            }
            if (!promise.isSuccess()) {
                throw new RuntimeException(promise.cause());
            }
            LOGGER.info("---Stream id: " + entry.getKey() + " received---");
            itr.remove();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        Integer streamId = msg.headers().getInt(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text());
        if (streamId == null) {
            LOGGER.error("HttpResponseHandler unexpected message received: " + msg);
            return;
        }

        Entry<ChannelFuture, ChannelPromise> entry = streamidPromiseMap.get(streamId);
        if (entry == null) {
            LOGGER.error("Message received for unknown stream id " + streamId);
        } else {
            // Do stuff with the message (for now just print it)
            ByteBuf content = msg.content();
            if (content.isReadable()) {
                int contentLength = content.readableBytes();
                byte[] arr = new byte[contentLength];
                content.readBytes(arr);
                this.lastResult = new String(arr, 0, contentLength, CharsetUtil.UTF_8);
            }

            entry.getValue().setSuccess();
        }
    }
}
