package com.proxychecker.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ProxyListDownloader {
    //private static final String PROXY_SERVERS_URL = "https://raw.githubusercontent.com/TheSpeedX/PROXY-List/master/socks4.txt";
    private static final String PROXY_SERVERS_URL_SOCKS4 = "https://www.proxy-list.download/api/v1/get?type=socks4";
    private static final String PROXY_SERVERS_URL_HTTPS = "https://www.proxy-list.download/api/v1/get?type=https";

    // Получает ip адреса серверов с сервера
    public static List<String> loadIpsSocks4Proxies() throws IOException, InterruptedException {
        return getProxies( PROXY_SERVERS_URL_SOCKS4 );
    }

    // Метод для получения SOCKS4 прокси через API
    public static List<String> loadIpsHttpsProxies() throws IOException, InterruptedException {
        return getProxies( PROXY_SERVERS_URL_HTTPS );
    }

    private static List<String> getProxies( String proxyServersUrlHttps ) throws IOException, InterruptedException {
        HttpResponse<String> response;
        try( HttpClient client = HttpClient.newHttpClient() ) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri( URI.create( proxyServersUrlHttps ) )
                    .build();

            response = client.send( request, HttpResponse.BodyHandlers.ofString() );
        }
        return parseProxies( response.body() );
    }

    private static List<String> parseProxies( String responseBody ) {
        // Разделяем по новой строке, обрабатываем и возвращаем список
        return Arrays.stream( responseBody.split( "\n" ) ) // Преобразуем в поток
                .map( String::trim ) // Убираем лишние пробелы
                .filter( line -> ! line.isEmpty() ) // Оставляем только непустые строки
                .collect( Collectors.toList() ); // Собираем результат в список
    }


}
