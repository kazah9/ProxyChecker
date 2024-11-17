package com.proxychecker;

import com.proxychecker.service.ProxyCheckerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProxyCheckerApplication implements CommandLineRunner {
    @Value( "${proxy:socks4}" )
    private String proxy;

    @Autowired
    private ProxyCheckerService proxyCheckerService;

    public static void main( String[] args ) {
        SpringApplication.run( ProxyCheckerApplication.class, args );
    }

    @Override
    public void run( String... args ) throws Exception {
        proxyCheckerService.checkProxies( proxy );
    }


}
