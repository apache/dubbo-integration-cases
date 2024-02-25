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
package org.dubbo.samples.test.consumer;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcServiceContext;

import java.util.concurrent.atomic.AtomicInteger;

@Activate(order = 50, group = {"consumer"})
public class CustomFilter implements Filter {

    private static final AtomicInteger invoked = new AtomicInteger(0);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        return invoker.invoke(invocation).whenCompleteWithContext(
                (r, t) -> {
                    RpcServiceContext serviceContext = RpcContext.getServiceContext();
                    String localAddress = serviceContext.getLocalAddressString();
                    String remoteAddress = serviceContext.getRemoteAddressString();
                    System.out.println("自定义日志local:" + localAddress);
                    System.out.println("自定义日志remote:" + remoteAddress);
                    int port = serviceContext.getLocalAddress().getPort();
                    invoked.set(port);
                }
        );
    }

    public static int expected() {
        return invoked.get();
    }

}
