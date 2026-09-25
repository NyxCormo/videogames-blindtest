package fr.insalan.blindtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest 
@ActiveProfiles("test")
public class SchemaTests {
    
    @Autowired 
    JdbcTemplate jdbc;

    @Test 
    void allTablesExist() {
        List<String> tables = jdbc.queryForList(
            "select name from sqlite_master where type = 'table'"
            + " and name not like 'sqlite_%' "
            + " and name <> 'flyway_schema_history' order by name",
            String.class
        );
		assertEquals(
            List.of(
                "blindtest",
                "blindtest_difficulty_band",
                "blindtest_score",
                "blindtest_track",
                "franchise",
                "game",
                "knowledge",
                "listener",
                "tag",
                "tag_type",
                "track",
                "track_tag"
            ),
            tables
        );
    }

    @Test
    void gameRequiresExistingFranchise() {
        //Spring ne traduit pas les erreurs SQLite, donc on catch l'exception et on vérifie le message
        assertThrows(DataAccessException.class, () -> {
            jdbc.update("insert into game (name, franchise_id) values (?, ?)", "Stellar Blade", 999);
        });
    }
}
