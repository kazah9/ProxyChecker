package com.proxychecker.controller;

import com.proxychecker.service.ProxyCheckerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URISyntaxException;

@RestController
@RequestMapping( "/api/proxy" )
public class ProxyCheckerController {

    private final ProxyCheckerService proxyCheckerService;

    public ProxyCheckerController( ProxyCheckerService proxyCheckerService ) {
        this.proxyCheckerService = proxyCheckerService;
    }

    @GetMapping( "/check" )
    public void checkProxies() throws IOException, InterruptedException, URISyntaxException {
        proxyCheckerService.checkProxies( "socks4" );
    }
}