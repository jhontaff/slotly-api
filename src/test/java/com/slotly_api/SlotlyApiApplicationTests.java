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
	void flywayCreatesTheBookingTable() {
		Integer rows = jdbcTemplate.queryForObject("select count(*) from booking", Integer.class);
		assertThat(rows).isZero();
	}

}
