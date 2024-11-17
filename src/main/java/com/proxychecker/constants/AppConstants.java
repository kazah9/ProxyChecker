package com.proxychecker.constants;

public class AppConstants {
    // API URLs
    public static final String TEST_URL                 = "http://httpbin.org/ip";
    public static final String GEOJS_API_URL            = "https://get.geojs.io/";
    public static final String IPINFO_API_URL           = "http://ipinfo.io/";
    public static final String PROXY_SERVERS_URL_HTTPS  = "https://www.proxy-list.download/api/v1/get?type=https";
    public static final String PROXY_SERVERS_URL_SOCKS4 = "https://www.proxy-list.download/api/v1/get?type=socks4";

    // GitHub URLs
    public static final String GITHUB_URL_HTTP   = "https://raw.githubusercontent.com/TheSpeedX/PROXY-List/master/http.txt";
    public static final String GITHUB_URL_SOCKS4 = "https://raw.githubusercontent.com/TheSpeedX/PROXY-List/master/socks4.txt";

    // Output Files
    public static final String OUTPUT_FILE = "proxies.txt";

    // Proxy Protocol Types
    public static final String PROTOCOL_HTTP  = "HTTP";
    public static final String PROTOCOL_HTTPS = "HTTPS";
    public static final String PROTOCOL_SOCKS = "SOCKS";

    public static final int CONNECT_TIMEOUT = 10;

    public static final int HTTP_PROXY_ERROR = -1;
}
