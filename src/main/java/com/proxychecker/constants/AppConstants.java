package com.proxychecker.constants;

public class AppConstants {
    // Threads
    public static final Integer THREAD_POOL = 256;

    // Resources
    public static final String RESOURCE_GITHUB = "GITHUB";
    public static final String RESOURCE_PROXY_LIST = "PROXY-LIST";

    // API URLs
    public static final String GEOJS_API_URL  = "https://get.geojs.io/";

    public static final String PROXY_LIST_URL_HTTP   = "https://www.proxy-list.download/api/v1/get?type=http";
    public static final String PROXY_LIST_URL_SOCKS4 = "https://www.proxy-list.download/api/v1/get?type=socks4";
    public static final String PROXY_LIST_URL_SOCKS5 = "https://www.proxy-list.download/api/v1/get?type=socks5";

    // GitHub URLs
    public static final String GITHUB_URL_HTTP   = "https://raw.githubusercontent.com/TheSpeedX/PROXY-List/master/http.txt";
    public static final String GITHUB_URL_SOCKS4 = "https://raw.githubusercontent.com/TheSpeedX/PROXY-List/master/socks4.txt";
    public static final String GITHUB_URL_SOCKS5 = "https://raw.githubusercontent.com/TheSpeedX/PROXY-List/master/socks5txt";

    // Proxy Protocol Types
    public static final String PROTOCOL_HTTP  = "HTTP";
    public static final String PROTOCOL_HTTPS = "HTTPS";
    public static final String PROTOCOL_SOCKS = "SOCKS";

    public static final int HTTP_PROXY_ERROR = -1;
}
