package com.dbpt2.server.tools;

import com.dbpt2.server.crypto.ResidentNoCipher;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * db/init/02_seed.sql의 주민번호 암호문을 .env의 RESIDENT_NO_KEY로 다시 만들고,
 * 비밀번호 해시가 demo1234와 맞지 않으면 새 해시로 바꾼다. 실행: ./gradlew -q seedSecrets
 */
public final class SeedSecrets {

    /** 프론트 residentNumber.ts 검사를 통과하는 가상 번호. employee_id 1~10 순서. */
    static final List<String> RESIDENT_NUMBERS = List.of(
            "900315-1000001", "920722-2000002", "910104-1000003", "950918-2000004", "880530-1000005",
            "980211-2000006", "970806-1000007", "000125-4000008", "930610-1000009", "961130-2000010");

    static final String DEMO_PASSWORD = "demo1234";

    public static void main(String[] args) throws Exception {
        Path seed = Path.of("db/init/02_seed.sql");
        String key = Files.readAllLines(Path.of(".env")).stream()
                .filter(line -> line.startsWith("RESIDENT_NO_KEY="))
                .map(line -> line.substring("RESIDENT_NO_KEY=".length()))
                .findFirst().orElseThrow(() -> new IllegalStateException(".env에 RESIDENT_NO_KEY가 없습니다."));
        ResidentNoCipher cipher = new ResidentNoCipher(key);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String sql = Files.readString(seed, StandardCharsets.UTF_8);
        Matcher hashes = Pattern.compile("'(\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53})'").matcher(sql);
        String newHash = null;
        while (hashes.find()) {
            if (!encoder.matches(DEMO_PASSWORD, hashes.group(1))) {
                newHash = newHash == null ? encoder.encode(DEMO_PASSWORD) : newHash;
            }
        }
        if (newHash != null) {
            sql = sql.replaceAll("'\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}'", Matcher.quoteReplacement("'" + newHash + "'"));
            System.out.println("비밀번호 해시가 demo1234와 맞지 않아 새로 만들었습니다.");
        } else {
            System.out.println("비밀번호 해시는 demo1234와 맞습니다. 그대로 둡니다.");
        }

        Matcher encrypted = Pattern.compile("'aesgcm:[A-Za-z0-9+/=]+'").matcher(sql);
        StringBuilder out = new StringBuilder();
        int index = 0;
        while (encrypted.find()) {
            encrypted.appendReplacement(out, Matcher.quoteReplacement("'" + cipher.encrypt(RESIDENT_NUMBERS.get(index++)) + "'"));
        }
        encrypted.appendTail(out);
        if (index != RESIDENT_NUMBERS.size()) {
            throw new IllegalStateException("암호문 개수가 " + index + "개입니다. 10개여야 합니다.");
        }
        Files.writeString(seed, out.toString(), StandardCharsets.UTF_8);
        System.out.println("주민번호 암호문 " + index + "개를 다시 만들었습니다.");
    }
}
