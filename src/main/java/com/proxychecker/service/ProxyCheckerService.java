package com.proxychecker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.proxychecker.constants.AppConstants.*;

@Service
public class ProxyCheckerService {
    private static final Logger logger = LoggerFactory.getLogger( ProxyCheckerService.class );

    private static final ExecutorService executor = Executors.newFixedThreadPool( THREAD_POOL ); // Ограничение потоков
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

    public void checkProxies( String flag ) throws Exception {
        // Открывает BufferedWriter для записи в файл
        try( BufferedWriter writer = new BufferedWriter( new FileWriter( OUTPUT_FILE_NAME, false ) ) ) {
            List<String> proxies;
            if( Objects.equals( flag, "http" ) ) {
                proxies = ProxyListDownloader.loadIpsHttpProxies();
            } else if( Objects.equals( flag, "socks4" ) ) {
                proxies = ProxyListDownloader.loadIpsSocks4Proxies();
            } else {
                logger.error( "Unknown proxy: {}", flag );
                throw new IllegalArgumentException( "Unknown proxy: " + flag );
            }

            logger.info( "Loaded {} proxies {}", proxies.size(), flag );

            // Создание списка CompletableFuture для асинхронной проверки каждого прокси
            List<CompletableFuture<Void>> futures = proxies.stream()
                    .map( proxy -> CompletableFuture.runAsync( () -> {
                        // Проверка прокси в отдельном потоке
                        checkAndLogProxy( proxy, writer );
                    }, executor ) ) // ограничивает потоки
                    .collect( Collectors.toList() );

            // Ожидание завершения всех асинхронных задач
            CompletableFuture<Void> allOf = CompletableFuture.allOf( futures.toArray( new CompletableFuture[0] ) );
            allOf.join(); // Блокирует до завершения всех задач

            executor.shutdown(); // После завершения задач закрывает пул потоков
        } catch( Exception e ) {
            logger.error( "Error while initializing file writer: {}", e.getMessage() );
        }
    }

    // Метод для проверки прокси и вывод результата по нему
    private void checkAndLogProxy( String proxy, BufferedWriter writer ) {
        final String country = getCountryByIp( proxy.split( ":" )[0] );
        if( "Unknown".equalsIgnoreCase( country ) ) {
            return;
        }

        final String proxyType = getProxyType( proxy );
        Optional.of( checkProxy( proxy ) )
                .filter( time -> time != HTTP_PROXY_ERROR )
                .ifPresent( time -> {
                    try {
                        // Записывает результат в файл
                        appendProxyInfo( writer, proxy, proxyType, time, country );
                    } catch( IOException e ) {
                        logger.error( "Error while writing proxy info to file: {}", e.getMessage() );
                    }
                } );
    }

    // Метод формирует основную информацию о прокси сервере
    private void appendProxyInfo( BufferedWriter writer, String proxy, String proxyType, long responseTime, String country ) throws IOException {
        // Преобразует время ответа в секунды и форматирует до 3 знаков
        double responseTimeInSeconds = responseTime / 1000.0;
        String formattedResponseTime = String.format( "%.3f", responseTimeInSeconds );

        String resultLine = proxy
                + " - "
                + proxyType
                + " - "
                + formattedResponseTime
                + "s - "
                + "Country: "
                + country;

        writer.write( resultLine );
        writer.newLine();

        logger.info( "Proxy {} is working. Type: {}, Response Time: {}s, Country: {}", proxy, proxyType, formattedResponseTime, country );
    }

    // Метод для проверки прокси
    private long checkProxy( String proxy ) {
        final long startTime = System.currentTimeMillis();

        try {
            final String[] parts = proxy.split( ":" );
            Socket socket = new Socket();
            SocketAddress socketAddress = new InetSocketAddress( parts[0], Integer.parseInt( parts[1] ) );

            // Попытка подключения к прокси-серверу, 3000 миллисекунд на попытку подключения
            socket.connect( socketAddress, 3000 );
            socket.close();

            // Возвращает время ответа если прокси работает
            return System.currentTimeMillis() - startTime;
        } catch( Exception e ) {
            logger.error( "Error while checking proxy {}: {}", proxy, e.toString() );
        }
        return HTTP_PROXY_ERROR;
    }

    // Метод для получения страны по IP
    private String getCountryByIp( String ip ) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri( URI.create( GEOJS_API_URL + "v1/ip/country/" + ip + ".json" ) )
                    .build();

            // Отправляет запрос и получает ответ
            HttpResponse<String> response = client.send( request, HttpResponse.BodyHandlers.ofString() );

            // Парсим JSON-ответ
            JsonNode jsonNode = objectMapper.readTree( response.body() );

            // Извлекает страну из ответа
            return jsonNode.path( "country" ).asText( "Unknown" );
        } catch( Exception e ) {
            logger.error( "Error while getting country for IP {}: {}", ip, e.getMessage() );
            return "Unknown";
        }
    }

    // Метод для получения типа прокси (SOCKS или HTTP)
    private String getProxyType( String proxy ) {
        // Извлекает порт из строки вида "ip:порт"
        final String port = proxy.split( ":" )[1];

        return switch( port ) {
            case "1080", "1081" -> PROTOCOL_SOCKS; // Порты 1080 и 1081 — для SOCKS-прокси
            case "80" -> PROTOCOL_HTTP;            // Порт 80 — это стандарт для HTTP
            case "443" -> PROTOCOL_HTTPS;          // Порт 443 — это стандарт для HTTPS
            default -> PROTOCOL_HTTP;              // Для всех остальных портов предполагает HTTP
        };
    }


}
