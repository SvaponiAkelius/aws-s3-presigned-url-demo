package com.akelius.svaponi.demo.utils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for encoding and decoding values according to RFC 5987.
 * See https://tools.ietf.org/html/rfc5987
 * See https://github.com/apache/cxf/blob/master/core/src/main/java/org/apache/cxf/attachment/Rfc5987Util.java
 */
public class Rfc5987 {

    private static final Pattern ENCODED_VALUE_PATTERN = Pattern.compile("%[0-9a-f]{2}|\\S", Pattern.CASE_INSENSITIVE);
    private static final char[] DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private static final byte[] ATTR_CHAR = {
            '!', '#', '$', '&', '+', '-', '.',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            '^', '_', '`',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            '|', '~'
    };

    public static String encodeUTF8(final String name) {
        return encode(Objects.requireNonNull(name).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @param bytes byte array
     * @return string encoded according to RFC 5987
     */
    public static String encode(final byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        final int len = bytes.length;
        final StringBuilder sb = new StringBuilder(len << 1);
        for (int i = 0; i < len; ++i) {
            final byte b = bytes[i];
            if (Arrays.binarySearch(ATTR_CHAR, b) >= 0) {
                sb.append((char) b);
            } else {
                sb.append('%');
                sb.append(DIGITS[0x0f & (b >>> 4)]);
                sb.append(DIGITS[b & 0x0f]);
            }
        }
        return sb.toString();
    }

    /**
     * @param input encoded input
     * @return byte array decoded according to RFC 5987
     */
    public static byte[] decode(final String input) {
        if (input == null) {
            return null;
        }
        final Matcher matcher = ENCODED_VALUE_PATTERN.matcher(input);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while (matcher.find()) {
            final String matched = matcher.group();
            if (matched.startsWith("%")) {
                final Integer value = Integer.parseInt(matched.substring(1), 16);
                bos.write(value);
            } else {
                bos.write(matched.charAt(0));
            }
        }
        return bos.toByteArray();
    }
}
