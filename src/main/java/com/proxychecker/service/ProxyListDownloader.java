package com.proxychecker.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.proxychecker.constants.AppConstants.*;

public class ProxyListDownloader {

    // Получает ip адреса серверов с сервера
    public static List<String> loadIpsSocks4Proxies() throws InterruptedException {
        return getProxies( GITHUB_URL_SOCKS4 );
    }

    // Метод для получения HTTP прокси через API
    public static List<String> loadIpsHttpProxies() throws InterruptedException {
        return getProxies( GITHUB_URL_HTTP );
    }

    private static List<String> getProxies( String proxyServersUrlHttps ) throws InterruptedException {
        HttpResponse<String> response;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri( URI.create( proxyServersUrlHttps ) )
                    .build();

            response = HttpClient.newHttpClient().send( request, HttpResponse.BodyHandlers.ofString() );
        } catch( IOException e ) {
            throw new RuntimeException( e );
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
