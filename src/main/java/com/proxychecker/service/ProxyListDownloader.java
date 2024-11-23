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
    public static List<String> loadProxies( String proxyType, String resource ) throws InterruptedException {
        return getProxies( determineProxyUrl( proxyType, resource ) );
    }

    // Получает ip адреса прокси из источника
    private static List<String> getProxies( String serverUrl ) throws InterruptedException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri( URI.create( serverUrl ) )
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

    private static String determineProxyUrl( String proxyType, String resource ) {
        return switch( resource.toLowerCase() ) {
            case "github" -> switch( proxyType.toLowerCase() ) {
                case "socks4" -> GITHUB_URL_SOCKS4;
                case "http" -> GITHUB_URL_HTTP;
                case "socks5" -> GITHUB_URL_SOCKS5;
                default -> throw new IllegalArgumentException( "Unsupported proxy type: " + proxyType );
            };
            case "proxy-list" -> switch( proxyType.toLowerCase() ) {
                case "socks4" -> PROXY_LIST_URL_SOCKS4;
                case "http" -> PROXY_LIST_URL_HTTP;
                case "socks5" -> PROXY_LIST_URL_SOCKS5;
                default -> throw new IllegalArgumentException( "Unsupported proxy type: " + proxyType );
            };
            default -> throw new IllegalArgumentException( "Unknown resource: " + resource );
        };
    }

}
