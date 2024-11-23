package com.proxychecker.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors( chain = true )
public class ProxyDto {

    @Schema( example = "192.168.1.1", description = "IP адрес прокси-сервера" )
    private String host;

    @Schema( example = "80", description = "Порт прокси-сервера" )
    private int port;

    @Schema( example = "HTTP", description = "Тип прокси" )
    private String proxyType;

    @Schema( example = "0,206", description = "Время отклика прокси в миллисекундах" )
    private BigDecimal responseTime;

    @Schema( example = "US", description = "Страна, в которой находится прокси" )
    private String country;

    public ProxyDto( String host, int port, String proxyType, BigDecimal responseTime, String country ) {
        this.host = host;
        this.port = port;
        this.proxyType = proxyType;
        this.responseTime = responseTime;
        this.country = country;
    }

    public String getHost() {
        return host;
    }

    public void setHost( String host ) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort( int port ) {
        this.port = port;
    }

    public String getProxyType() {
        return proxyType;
    }

    public void setProxyType( String proxyType ) {
        this.proxyType = proxyType;
    }

    public BigDecimal getResponseTime() {
        return responseTime;
    }

    public void setResponseTime( BigDecimal responseTime ) {
        this.responseTime = responseTime;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry( String country ) {
        this.country = country;
    }
}
