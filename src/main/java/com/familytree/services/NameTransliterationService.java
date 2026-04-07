package com.familytree.services;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class NameTransliterationService {

    private static final Map<String, String> DIGRAPHS = new LinkedHashMap<>();
    private static final Map<Character, String> LETTERS = new LinkedHashMap<>();

    static {
        DIGRAPHS.put("ksh", "क्ष");
        DIGRAPHS.put("chh", "छ");
        DIGRAPHS.put("ch", "च");
        DIGRAPHS.put("sh", "श");
        DIGRAPHS.put("th", "थ");
        DIGRAPHS.put("dh", "ध");
        DIGRAPHS.put("ph", "फ");
        DIGRAPHS.put("bh", "भ");
        DIGRAPHS.put("kh", "ख");
        DIGRAPHS.put("gh", "घ");
        DIGRAPHS.put("jh", "झ");
        DIGRAPHS.put("ny", "ञ");
        DIGRAPHS.put("ng", "ङ");
        DIGRAPHS.put("aa", "आ");
        DIGRAPHS.put("ee", "ई");
        DIGRAPHS.put("ii", "ई");
        DIGRAPHS.put("oo", "ऊ");
        DIGRAPHS.put("ou", "औ");
        DIGRAPHS.put("ai", "ऐ");
        DIGRAPHS.put("au", "औ");

        LETTERS.put('a', "अ");
        LETTERS.put('b', "ब");
        LETTERS.put('c', "क");
        LETTERS.put('d', "द");
        LETTERS.put('e', "ए");
        LETTERS.put('f', "फ");
        LETTERS.put('g', "ग");
        LETTERS.put('h', "ह");
        LETTERS.put('i', "इ");
        LETTERS.put('j', "ज");
        LETTERS.put('k', "क");
        LETTERS.put('l', "ल");
        LETTERS.put('m', "म");
        LETTERS.put('n', "न");
        LETTERS.put('o', "ओ");
        LETTERS.put('p', "प");
        LETTERS.put('q', "क");
        LETTERS.put('r', "र");
        LETTERS.put('s', "स");
        LETTERS.put('t', "त");
        LETTERS.put('u', "उ");
        LETTERS.put('v', "व");
        LETTERS.put('w', "व");
        LETTERS.put('x', "क्स");
        LETTERS.put('y', "य");
        LETTERS.put('z', "ज");
    }

    public String transliterate(String text) {
        if (text == null) {
            return null;
        }

        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        String lower = trimmed.toLowerCase();
        int index = 0;

        while (index < lower.length()) {
            char current = lower.charAt(index);

            if (Character.isWhitespace(current)) {
                result.append(' ');
                index++;
                continue;
            }

            if (!Character.isLetter(current)) {
                result.append(trimmed.charAt(index));
                index++;
                continue;
            }

            String matched = null;
            String replacement = null;

            for (Map.Entry<String, String> entry : DIGRAPHS.entrySet()) {
                if (lower.startsWith(entry.getKey(), index)) {
                    matched = entry.getKey();
                    replacement = entry.getValue();
                    break;
                }
            }

            if (matched != null) {
                result.append(replacement);
                index += matched.length();
                continue;
            }

            result.append(LETTERS.getOrDefault(current, String.valueOf(trimmed.charAt(index))));
            index++;
        }

        return cleanup(result.toString());
    }

    private String cleanup(String text) {
        return text
                .replace("भअट्टअ", "भट्ट")
                .replace("भअट्ट", "भट्ट")
                .replace("नअथ", "नाथ")
                .replace("प्रअसअद", "प्रसाद")
                .replace("किशओर", "किशोर")
                .replace("ज्ह", "झ")
                .replace("छ्ह", "छ")
                .replace("  ", " ")
                .trim();
    }
}
