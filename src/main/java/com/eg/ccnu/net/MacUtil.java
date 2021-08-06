package com.eg.ccnu.net;

import cn.hutool.core.util.RandomUtil;

public class MacUtil {
    public static char randomHexChar() {
        return RandomUtil.randomChar("0123456789abcdef");
    }

    public static String generateMac() {
        StringBuilder mac = new StringBuilder(17);

        mac.append(randomHexChar()).append(randomHexChar());
        for (int i = 0; i < 5; i++) {
            mac.append(":").append(randomHexChar()).append(randomHexChar());
        }
        return mac.toString();
    }

}
