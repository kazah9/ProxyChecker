package com.proxychecker.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ProxyListDownloader {

    public static void main(String[] args) {
        try {
            // URL сырого файла с прокси
            String fileUrl = "https://raw.githubusercontent.com/TheSpeedX/PROXY-List/master/socks4.txt";

            // Создаем URL-объект
            URL url = new URL(fileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);  // Тайм-аут на подключение
            connection.setReadTimeout(5000);     // Тайм-аут на чтение

            // Чтение содержимого файла
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine).append("\n");
            }
            in.close();

            // Выводим содержимое файла
            System.out.println(content.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
