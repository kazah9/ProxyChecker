package com.proxychecker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proxychecker.ProxyCheckerApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        StringBuilder result = new StringBuilder();

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
                    checkAndLogProxy( proxy, result );
                }, executor ) ) // Используем executor для ограничения потоков
                .collect( Collectors.toList() );

        // Ожидание завершения всех асинхронных задач
        CompletableFuture<Void> allOf = CompletableFuture.allOf( futures.toArray( new CompletableFuture[0] ) );
        allOf.join(); // Блокирует до завершения всех задач
        executor.shutdown(); // После завершения всех задач закрываем пул потоков
        //
        if( ! result.isEmpty() ) {
            writeResultsToFile( result.toString() );
        }
    }

    // Метод для проверки прокси и вывод результата по нему
    private void checkAndLogProxy( String proxy, StringBuilder result ) {
        final String country = getCountryByIp( proxy.split( ":" )[0] );
        if( "Unknown".equalsIgnoreCase( country ) ) {
            return;
        }

        final String proxyType = getProxyType( proxy );
        Optional.of( checkProxy( proxy ) )
                .filter( time -> time != HTTP_PROXY_ERROR )
                .ifPresent( time -> appendProxyInfo( result, proxy, proxyType, time, country ) );
    }

    // Метод формирует основную информацию о прокси сервере
    private void appendProxyInfo( StringBuilder result, String proxy, String proxyType, long responseTime, String country ) {
        // Преобразуем время ответа в секунды и форматируем до 3 знаков
        double responseTimeInSeconds = responseTime / 1000.0;
        String formattedResponseTime = String.format( "%.3f", responseTimeInSeconds );

        result.append( proxy )
                .append( " - " ).append( proxyType )
                .append( " - " ).append( formattedResponseTime ).append( "s - " )
                .append( "Country: " ).append( country )
                .append( "\n" );

        logger.info( "Proxy {} is working. Type: {}, Response Time: {}s, Country: {}", proxy, proxyType, formattedResponseTime, country );
    }

    // Метод для проверки прокси
    private long checkProxy( String proxy ) {
        final long startTime = System.currentTimeMillis();

        try {
            final String[] parts = proxy.split( ":" );
            Socket socket = new Socket();
            SocketAddress socketAddress = new InetSocketAddress( parts[0], Integer.parseInt( parts[1] ) );

            // Пробуем подключиться к прокси-серверу, 3000 миллисекунд на попытку подключения
            socket.connect( socketAddress, 3000 );
            socket.close();

            // Возвращаем время ответа если прокси работает
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

            // Отправляем запрос и получаем ответ
            HttpResponse<String> response = client.send( request, HttpResponse.BodyHandlers.ofString() );

            // Парсим JSON-ответ
            JsonNode jsonNode = objectMapper.readTree( response.body() );

            // Извлекаем страну из ответа
            return jsonNode.path( "country" ).asText( "Unknown" );
        } catch( Exception e ) {
            logger.error( "Error while getting country for IP {}: {}", ip, e.getMessage() );
            return "Unknown";
        }
    }

    // Метод для получения типа прокси (SOCKS или HTTP)
    private String getProxyType( String proxy ) {
        // Извлекаем порт из строки вида "ip:порт"
        final String port = proxy.split( ":" )[1];

        return switch( port ) {
            case "1080", "1081" -> PROTOCOL_SOCKS; // Порты 1080 и 1081 — для SOCKS-прокси
            case "80" -> PROTOCOL_HTTP;            // Порт 80 — это стандарт для HTTP
            case "443" -> PROTOCOL_HTTPS;          // Порт 443 — это стандарт для HTTPS
            default -> PROTOCOL_HTTP;              // Для всех остальных портов предполагаем HTTP
        };
    }

    // Метод для записи результатов в файл
    private void writeResultsToFile( String results ) throws Exception {
        File file;
        // Проверяем, если приложение запущено из IDE
        if( isRunningInIDE() ) {
            file = new File( OUTPUT_FILE_NAME );
        } else {
            Path jarDir = getJarDirectory();
            file = new File( jarDir.toFile(), OUTPUT_FILE_NAME );
        }
        logger.info( "Path file {}", file );
        try( BufferedWriter writer = new BufferedWriter( new FileWriter( file, false ) ) ) {
            writer.write( results );
            writer.newLine();
        }
    }

    // Метод для проверки, запущено ли приложение из IDE
    private boolean isRunningInIDE() {
        logger.info( "Checking if ProxyChecker is running in IDE" );
        return ! System.getProperty( "user.dir" ).contains( "target" );
    }

    // Метод для проверки, запущено ли приложение из JAR файла
    private Path getJarDirectory() {
        try {
            // Получаем путь к JAR файлу
            String path = ProxyCheckerApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();

            // Возвращаем родительскую директорию, где находится JAR
            return new File( path ).getParentFile().toPath();
        } catch( Exception e ) {
            // В случае ошибки возвращаем текущую директорию
            logger.error( "Error while getting jar directory: {}", e.getMessage() );
            return Paths.get( "." );
        }
    }


}
