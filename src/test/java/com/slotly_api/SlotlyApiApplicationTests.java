package com.slotly_api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SlotlyApiApplicationTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void flywayCreatesTheSchema() {
		assertThat(tableExists("booking")).isTrue();
		assertThat(tableExists("provider")).isTrue();
	}

	private boolean tableExists(String table) {
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject("select to_regclass(?) is not null", Boolean.class, table));
	}

}
