package com.proxychecker.controller;

import com.proxychecker.service.ProxyCheckerService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping( "/api/proxy" )
public class ProxyCheckerController {

    private final ProxyCheckerService proxyCheckerService;

    public ProxyCheckerController( ProxyCheckerService proxyCheckerService ) {
        this.proxyCheckerService = proxyCheckerService;
    }

    @GetMapping( "/check" )
    public String checkProxies() throws IOException, InterruptedException {
        return proxyCheckerService.checkProxies();
    }
}