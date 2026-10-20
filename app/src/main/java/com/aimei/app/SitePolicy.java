package com.aimei.app;

import java.net.URI;
import java.net.URISyntaxException;

/** Restricts camera permission and in-app navigation to the real Anywear origin. */
public final class SitePolicy {
    public static final String TRY_ON_URL = "https://anywear.decart.ai/?try=1";

    private SitePolicy() { }

    public static boolean isTrusted(String address) {
        if (address == null) return false;
        try {
            URI uri = new URI(address);
            return "https".equalsIgnoreCase(uri.getScheme())
                && "anywear.decart.ai".equalsIgnoreCase(uri.getHost())
                && uri.getUserInfo() == null
                && (uri.getPort() == -1 || uri.getPort() == 443);
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    public static boolean isExternalWeb(String address) {
        if (address == null) return false;
        try {
            URI uri = new URI(address);
            return "https".equalsIgnoreCase(uri.getScheme())
                && uri.getHost() != null && uri.getUserInfo() == null;
        } catch (URISyntaxException exception) {
            return false;
        }
    }
}
