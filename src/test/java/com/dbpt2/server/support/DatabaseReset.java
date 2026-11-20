package com.dbpt2.server.support;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/** scripts/reset-db.sh와 같은 방식: DB는 두고 다섯 테이블을 지운 뒤 db/init 스크립트를 다시 실행한다. */
public final class DatabaseReset {

    private DatabaseReset() {}

    public static void reset(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("SET FOREIGN_KEY_CHECKS=0");
            statement.execute("DROP TABLE IF EXISTS EVALUATION, PROJECT_MEMBER, PROJECT, CLIENT, EMPLOYEE");
            statement.execute("SET FOREIGN_KEY_CHECKS=1");
            for (String file : new String[] {"db/init/01_schema.sql", "db/init/02_seed.sql"}) {
                // 권한이 DB 단위인 앱 계정으로도 실행되도록 CREATE DATABASE·USE 줄은 뺀다.
                String sql = Files.readAllLines(Path.of(file), StandardCharsets.UTF_8).stream()
                        .filter(line -> !line.startsWith("CREATE DATABASE") && !line.startsWith("USE "))
                        .collect(Collectors.joining("\n"));
                ScriptUtils.executeSqlScript(connection, new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)));
            }
        } catch (Exception e) {
            throw new IllegalStateException("DB 초기화 실패", e);
        }
    }
}
