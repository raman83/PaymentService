package com.payment.util;

public final class ProxyUtils {
    private ProxyUtils() {}

    public static String normalizeProxy(String proxyType, String proxyValue) {
        if (proxyType == null || proxyValue == null) return proxyValue;
        if ("PHONE".equalsIgnoreCase(proxyType)) {
            String digits = proxyValue.replaceAll("[^0-9+]", "");
            if (!digits.startsWith("+")) {
                // default to +1 (Canada/US) if missing
                digits = "+1" + digits;
            }
            return digits;
        }
        if ("EMAIL".equalsIgnoreCase(proxyType)) {
            return proxyValue.trim().toLowerCase();
        }
        return proxyValue.trim();
    }
}
