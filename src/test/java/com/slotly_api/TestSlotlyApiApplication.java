package com.slotly_api;

import org.springframework.boot.SpringApplication;

public class TestSlotlyApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(SlotlyApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
