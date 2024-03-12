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

import org.apache.dubbo.samples.api.QosService;

import org.apache.commons.io.FileUtils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class QosServiceImpl implements QosService {
    @Override
    public long usedMemory() {
        String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        new File("/tmp/analyze").mkdirs();
        try {
            Process process = Runtime.getRuntime().exec(
                    String.format("jmap -dump:format=b,file=/tmp/analyze/dump.bin,live %s", pid)
            );
            StringBuilder stringBuilder = new StringBuilder();
            InputStreamReader streamReader = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8);
            do {
                char[] chars = new char[1024];
                int len;
                while ((len = streamReader.read(chars)) != -1) {
                    stringBuilder.append(chars, 0, len);
                }
            } while (process.isAlive());

            process = Runtime.getRuntime().exec(
                    String.format("/usr/local/mat/ParseHeapDump.sh /tmp/analyze/dump.bin -format=csv org.eclipse.mat.api:overview", pid)
            );
            stringBuilder = new StringBuilder();
            streamReader = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8);
            do {
                char[] chars = new char[1024];
                int len;
                while ((len = streamReader.read(chars)) != -1) {
                    stringBuilder.append(chars, 0, len);
                }
            } while (process.isAlive());

            long size = -1;
            ZipFile zf = new ZipFile("/tmp/analyze/dump_System_Overview.zip");
            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(Paths.get("/tmp/analyze/dump_System_Overview.zip")))) {
                ZipEntry entry = zis.getNextEntry();
                while (entry != null) {
                    String entryName = entry.getName();
                    if (entryName.equals("pages/Heap_Dump_Overview2.csv")) {
                        BufferedReader br = new BufferedReader(
                                new InputStreamReader(zf.getInputStream(entry)));
                        String line;
                        while ((line = br.readLine()) != null) {
                            System.out.println(line);
                            if (line.startsWith("Number of objects")) {
                                size = Long.parseLong(line.split("\"")[1].replaceAll(",", "").trim());
                            }
                        }
                        br.close();
                        break;
                    }
                    entry = zis.getNextEntry();
                }
            }

            FileUtils.deleteDirectory(new File("/tmp/analyze"));
            if (size > -1) {
                System.out.println("size: " + size);
                return size;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return Runtime.getRuntime().maxMemory() - Runtime.getRuntime().freeMemory();
    }

}
