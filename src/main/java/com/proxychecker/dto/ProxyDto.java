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
}
