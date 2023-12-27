package org.apache.dubbo.samples.provider;

import org.apache.dubbo.samples.api.GreetingsService;

public class GreetingsServiceImpl implements GreetingsService {
    @Override
    public String echo1(String data) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return data;
    }

    @Override
    public String echo2(String data) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return data;
    }

}
