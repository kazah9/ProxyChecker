package com.proxychecker.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.proxychecker.constants.AppConstants.GITHUB_URL_HTTP;
import static com.proxychecker.constants.AppConstants.GITHUB_URL_SOCKS4;

public class ProxyListDownloader {

    // Получает ip адреса серверов с сервера
    public static List<String> loadIpsSocks4Proxies() throws InterruptedException {
        return getProxies( GITHUB_URL_SOCKS4 );
    }

    // Метод для получения HTTP прокси через API
    public static List<String> loadIpsHttpProxies() throws InterruptedException {
        return getProxies( GITHUB_URL_HTTP );
    }

    // Получает ip адреса прокси из источника
    private static List<String> getProxies( String proxyServersUrlHttps ) throws InterruptedException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri( URI.create( proxyServersUrlHttps ) )
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send( request, HttpResponse.BodyHandlers.ofString() );
            return parseProxies( response.body() );
        } catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    // Парсит ответ от источника с ip адресами
    private static List<String> parseProxies( String responseBody ) {
        // Разделяет по новой строке, обрабатывает и возвращает список
        return Arrays.stream( responseBody.split( "\n" ) ) // Преобразует в поток
                .map( String::trim ) // Убирает лишние пробелы
                .filter( line -> ! line.isEmpty() ) // Оставляет только непустые строки
                .collect( Collectors.toList() ); // Собирает результат в список
    }


}
