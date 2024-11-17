package com.proxychecker.controller;

import java.util.List;

/**
 * Класс обертка для данных в формате JSON
 */
public class ProxyRequest {

    private List<String> proxies;

    public List<String> getProxies() {
        return proxies;
    }

    public void setProxies( List<String> proxies ) {
        this.proxies = proxies;
    }
}
