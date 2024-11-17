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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProxyCheckerService {
    private static final Logger logger = LoggerFactory.getLogger( ProxyCheckerService.class );

    private static final String TEST_URL = "http://httpbin.org/ip";  // Используем httpbin для тестирования IP
    private static final String OUTPUT_FILE = "working_proxies.txt"; // Файл для рабочих прокси
    private static final String IPINFO_API_URL = "http://ipinfo.io/";
    private static final String GEOJS_API_URL = "https://get.geojs.io/";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

    public String checkProxies() throws IOException, InterruptedException {
        StringBuilder result = new StringBuilder();

        final List<String> proxies = loadIpsFromFile();
        logger.info( "Loaded {} proxies", proxies.size() );

        // Используем Vavr List для доступа к индексу
        io.vavr.collection.List<String> vavrProxies = io.vavr.collection.List.ofAll( proxies );
        vavrProxies.forEachWithIndex( ( proxy, index ) -> {
            logger.info( "Checking proxy {} of {}: {}", index + 1, vavrProxies.size(), proxy );
            // Получаем страну по IP
            final String country = getCountryByIp( proxy.split( ":" )[0] );
            if( "Unknown".equalsIgnoreCase( country ) ) {
                return;
            }

            // Получаем тип прокси (SOCKS или HTTP)
            String proxyType = getProxyType( proxy );

            // Проверяем прокси и получаем его responseTime
            Optional<Long> responseTimeOptional = Optional.of( checkProxy( proxy ) );

            // Если прокси работает (responseTime > 0), выполняем дальнейшие действия
            responseTimeOptional.filter( responseTime -> responseTime != - 1 ).ifPresent( responseTime -> {

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
            e.printStackTrace();
        }

        return result.toString();
    }

    // Пример основного метода
    private List<String> loadIpsFromFile() throws IOException, InterruptedException {
        ProxyCheckerService service = new ProxyCheckerService();

        // URL сырого файла с прокси
        String fileUrl = "https://raw.githubusercontent.com/TheSpeedX/PROXY-List/master/socks4.txt";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri( URI.create( fileUrl ) )
                .build();

        HttpResponse<String> response = client.send( request, HttpResponse.BodyHandlers.ofString() );
        return parseProxies( response.body() ); // Выводим содержимое
    }

    // Метод для парсинга строки с прокси в список
    private List<String> parseProxies( String responseBody ) {
        // Разделяем по новой строке, обрабатываем и возвращаем список
        return Arrays.stream( responseBody.split( "\n" ) ) // Преобразуем в поток
                .map( String::trim ) // Убираем лишние пробелы
                .filter( line -> ! line.isEmpty() ) // Оставляем только непустые строки
                .collect( Collectors.toList() ); // Собираем результат в список
    }

    // Метод для получения типа прокси (SOCKS или HTTP)
    private String getProxyType( String proxy ) {
        String port = proxy.split( ":" )[1];
        if( "1080".equals( port ) || "1081".equals( port ) ) {
            return "SOCKS";
        } else {
            return "HTTP";
        }
    }

    // Метод для проверки прокси
    private long checkProxy( String proxy ) {
        long startTime = System.currentTimeMillis();  // Начало времени

        try {
            // Формируем URI прокси
            URI uri = URI.create( TEST_URL );

            // Создаем запрос
            HttpRequest request = HttpRequest.newBuilder()
                    .uri( uri )
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
            System.out.println( "Error while checking proxy " + proxy + ": " + e.getMessage() );
            e.printStackTrace();
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
            System.out.println( "Error while getting country for IP " + ip + ": " + e.getMessage() );
            return "Unknown"; // В случае ошибки возвращаем "Unknown"
        }
    }

    // Метод для записи результатов в файл
    private void writeResultsToFile( String results ) throws IOException {
        File file = new File( OUTPUT_FILE );
        try( BufferedWriter writer = new BufferedWriter( new FileWriter( file, true ) ) ) {
            writer.write( results );
            writer.newLine();
        }
    }


}
