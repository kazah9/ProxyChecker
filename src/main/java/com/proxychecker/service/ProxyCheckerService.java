package com.proxychecker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

@Service
public class ProxyCheckerService {
    private static final Logger logger = LoggerFactory.getLogger( ProxyCheckerService.class );

    private static final String TEST_URL = "http://httpbin.org/ip";  // Используем httpbin для тестирования IP
    private static final String OUTPUT_FILE = "working_proxies.txt"; // Файл для рабочих прокси
    private static final String IPINFO_API_URL = "http://ipinfo.io/";
    private static final String GEOJS_API_URL = "https://get.geojs.io/";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

    public void checkProxies() throws IOException, InterruptedException {
        StringBuilder result = new StringBuilder();

        final List<String> proxies = ProxyListDownloader.loadIpsHttpsProxies();
        logger.info( "Loaded {} proxies", proxies.size() );

        // Используем Vavr List для доступа к индексу
        io.vavr.collection.List<String> vavrProxies = io.vavr.collection.List.ofAll( proxies );
        vavrProxies.forEachWithIndex( ( proxy, index ) -> {
            logger.info( "Checking proxy {} of {}: {}", index + 1, vavrProxies.size(), proxy );
            final String proxyType = getProxyType( proxy );
            final String country = getCountryByIp( proxy.split( ":" )[0] );
            if( "Unknown".equalsIgnoreCase( country ) ) {
                return;
            }
            // Если прокси работает (responseTime > 0), выполняем дальнейшие действия
            Optional.of( checkProxy( proxy ) )
                    .filter( responseTime -> responseTime != - 1 )
                    .ifPresent( responseTime -> {
                        // Преобразуем время ответа в секунды и форматируем до 3 знаков
                        double responseTimeInSeconds = responseTime / 1000.0;
                        String formattedResponseTime = String.format( "%.3f", responseTimeInSeconds );
                        // Добавляем результат в StringBuilder
                        result.append( proxy )
                                .append( " - " )
                                .append( proxyType )
                                .append( " - " )
                                .append( formattedResponseTime )
                                .append( "s - " )
                                .append( "Country: " ).append( country )
                                .append( "\n" );

                        logger.info( "Proxy {} is working. Type: {}, Response Time: {}s, Country: {}", proxy, proxyType, formattedResponseTime, country );
                    } );
        } );

        // Записываем рабочие прокси в файл
        try {
            writeResultsToFile( result.toString() );
        } catch( IOException e ) {
            logger.error( "Error write results to file {}", e.getMessage() );
        }
    }

    // Метод для получения типа прокси (SOCKS или HTTP)
    private String getProxyType( String proxy ) {
        // Извлекаем порт из строки вида "ip:порт"
        String port = proxy.split( ":" )[1];

        // Используем switch для определения типа прокси по порту
        switch( port ) {
            case "1080":
            case "1081":
                return "SOCKS";  // Порты 1080 и 1081 — для SOCKS-прокси
            case "80":
                return "HTTP";   // Порт 80 — это стандарт для HTTP
            case "443":
                return "HTTPS";  // Порт 443 — это стандарт для HTTPS
            default:
                return "HTTP";   // Для всех остальных портов предполагаем HTTP
        }
    }

    // Метод для проверки прокси
    private long checkProxy( String proxy ) {
        final long startTime = System.currentTimeMillis();

        try {
            // Создаем запрос
            HttpRequest request = HttpRequest.newBuilder()
                    .uri( URI.create( TEST_URL ) )
                    .header( "Proxy", proxy )
                    .build();

            // Отправляем запрос и получаем ответ
            HttpResponse<String> response = client.send( request, HttpResponse.BodyHandlers.ofString() );

            // Время ответа
            long responseTime = System.currentTimeMillis() - startTime;
            // Проверяем статус ответа (200 OK)
            if( response.statusCode() == 200 ) {
                return responseTime; // Возвращаем время ответа, если прокси работает
            }
        } catch( IOException | InterruptedException e ) {
            logger.error( "Error while checking proxy {}: {}", proxy, e.getMessage() );
        }
        return - 1; // Прокси не работает
    }

    // Метод для получения страны по IP
    private String getCountryByIp( String ip ) {
        try {
            // Создаем запрос
            HttpRequest request = HttpRequest.newBuilder()
                    .uri( URI.create( GEOJS_API_URL + "v1/ip/country/" + ip + ".json" ) )
                    .build();

            // Отправляем запрос и получаем ответ
            HttpResponse<String> response = client.send( request, HttpResponse.BodyHandlers.ofString() );

            // Получаем содержимое ответа
            String jsonResponse = response.body();

            // Парсим JSON-ответ
            JsonNode jsonNode = objectMapper.readTree( jsonResponse );

            // Извлекаем страну из ответа
            return jsonNode.path( "country" ).asText( "Unknown" ); // Возвращаем страну или "Unknown"

        } catch( IOException | InterruptedException e ) {
            logger.error( "Error while getting country for IP {}: {}", ip, e.getMessage() );
            return "Unknown"; // В случае ошибки возвращаем "Unknown"
        }
    }

    // Метод для записи результатов в файл
    private void writeResultsToFile( String results ) throws IOException {
        File file = new File( OUTPUT_FILE );
        try( BufferedWriter writer = new BufferedWriter( new FileWriter( file, false ) ) ) {
            writer.write( results );
            writer.newLine();
        }
    }


}
