package com.proxychecker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proxychecker.dto.ProxyDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.proxychecker.constants.AppConstants.*;

@Service
public class ProxyCheckerService {

    private static final Logger logger = LoggerFactory.getLogger( ProxyCheckerService.class );
    private static ExecutorService executor = Executors.newFixedThreadPool( THREAD_POOL ); // Ограничение потоков
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

    public List<ProxyDto> checkProxies( String flag, String resource ) {
        try {
            List<String> proxies = getProxies( flag, resource );
            return checkProxiesAsync( proxies );
        } catch( Exception e ) {
            logger.error( "Error during proxy checking process: {}", e.getMessage() );
            throw new RuntimeException( "Error during proxy checking process: " + e.getMessage() );
        }
    }

    private List<String> getProxies( String flag, String resource ) throws Exception {
        isExecutorTerminated();

        List<String> proxies = ProxyListDownloader.loadProxies( flag.toLowerCase(), resource );

        logger.info( "Loaded {} proxies for flag '{}'", proxies.size(), flag );
        return proxies;
    }

    private List<ProxyDto> checkProxiesAsync( List<String> proxies ) {
        // Создаем список CompletableFuture для асинхронной проверки каждого прокси
        List<CompletableFuture<ProxyDto>> futures = proxies.stream()
                .map( proxy -> CompletableFuture.supplyAsync( () -> checkAndCreateProxyDto( proxy ), executor ) )
                .toList();

        // Ожидаем завершения всех асинхронных задач и собираем результаты
        return futures.stream()
                .map( CompletableFuture::join )
                .filter( Objects::nonNull )
                .collect( Collectors.toList() );
    }

    private ProxyDto checkAndCreateProxyDto( String proxy ) {
        String[] proxyParts = proxy.split( ":" );
        if( proxyParts.length != 2 ) {
            logger.warn( "Invalid proxy format: {}", proxy );
            return null; // Неправильный формат прокси
        }

        final String host = proxyParts[0];
        final int port;
        try {
            port = Integer.parseInt( proxyParts[1] );
        } catch( NumberFormatException e ) {
            logger.warn( "Invalid port in proxy {}: {}", proxy, proxyParts[1] );
            return null; // Неправильный формат порта
        }

        // Проверка доступности прокси
        final long responseTime = checkProxy( proxy );
        if( responseTime == HTTP_PROXY_ERROR ) {
            return null; // Прокси не работает, пропускаем
        }

        // Получаем страну по IP
        final String country = getCountryByIp( host );
        if( "Unknown".equalsIgnoreCase( country ) ) {
            return null; // Если страна не определена, пропускаем этот прокси
        }

        // Определяем тип прокси
        final String proxyType = getProxyType( proxy );

        // Преобразуем время отклика в секунды и округляем до 3 знаков
        final BigDecimal responseTimeInSeconds = BigDecimal.valueOf( responseTime / 1000.0 )
                .setScale( 3, RoundingMode.HALF_UP );

        // Логирование успешной проверки
        logger.info( "Proxy {} is working. Type: {}, Response Time: {}s, Country: {}", proxy, proxyType, responseTimeInSeconds, country );
        return new ProxyDto()
                .setHost( host )
                .setPort( port )
                .setProxyType( proxyType )
                .setResponseTime( responseTimeInSeconds )
                .setCountry( country );
    }

    private long checkProxy( String proxy ) {
        final long startTime = System.currentTimeMillis();
        try {
            final String[] parts = proxy.split( ":" );
            Socket socket = new Socket();
            SocketAddress socketAddress = new InetSocketAddress( parts[0], Integer.parseInt( parts[1] ) );
            socket.connect( socketAddress, 3000 );
            socket.close();

            return System.currentTimeMillis() - startTime;
        } catch( Exception e ) {
            logger.error( "Error while checking proxy {}: {}", proxy, e.toString() );
            return HTTP_PROXY_ERROR;
        }
    }

    private String getCountryByIp( String ip ) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri( URI.create( GEOJS_API_URL + "v1/ip/country/" + ip + ".json" ) )
                    .build();

            HttpResponse<String> response = client.send( request, HttpResponse.BodyHandlers.ofString() );
            JsonNode jsonNode = objectMapper.readTree( response.body() );

            return jsonNode.path( "country" ).asText( "Unknown" );
        } catch( Exception e ) {
            logger.error( "Error while getting country for IP {}: {}", ip, e.getMessage() );
            return "Unknown";
        }
    }

    private String getProxyType( String proxy ) {
        final String port = proxy.split( ":" )[1];

        return switch( port ) {
            case "1080", "1081" -> PROTOCOL_SOCKS; // Порты 1080 и 1081 — для SOCKS-прокси
            case "80" -> PROTOCOL_HTTP;            // Порт 80 — это стандарт для HTTP
            case "443" -> PROTOCOL_HTTPS;          // Порт 443 — это стандарт для HTTPS
            default -> PROTOCOL_HTTP;              // Для всех остальных портов предполагает HTTP
        };
    }

    // Проверка завершения старого пула потоков
    private void isExecutorTerminated() {
        if( executor.isTerminated() ) {
            executor = Executors.newFixedThreadPool( THREAD_POOL );
        }
    }
}
