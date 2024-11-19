package com.proxychecker.controller;

import com.proxychecker.service.ProxyCheckerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
     *
     * @param typeProxy - тип прокси
     */
    @GetMapping( "/check/{typeProxy}" )
    public ResponseEntity<String> checkProxies( @PathVariable String typeProxy ) {
        try {
            proxyCheckerService.checkProxies( typeProxy );
            return ResponseEntity.ok( "Proxies checked successfully" );
        } catch( Exception e ) {
            return ResponseEntity.status( HttpStatus.INTERNAL_SERVER_ERROR )
                    .body( e.getMessage() );
        }
    }
}