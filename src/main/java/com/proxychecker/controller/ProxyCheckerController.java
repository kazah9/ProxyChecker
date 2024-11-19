package com.proxychecker.controller;

import com.proxychecker.service.ProxyCheckerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping( "/api/proxy" )
public class ProxyCheckerController {

    private final ProxyCheckerService proxyCheckerService;

    public ProxyCheckerController( ProxyCheckerService proxyCheckerService ) {
        this.proxyCheckerService = proxyCheckerService;
    }

    /**
     * Проверка прокси серверов из открытого источника по типу прокси
     * @param typeProxy - тип прокси
     */
    @GetMapping( "/check/{typeProxy}" )
    public void checkProxies( @PathVariable String typeProxy ) throws Exception {
        try {
            proxyCheckerService.checkProxies( typeProxy );
        } catch( Exception e ) {
            throw new Exception( e );
        }
    }
}