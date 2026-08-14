package com.foodmap;

import org.springframework.boot.SpringApplication;

public class TestFoodmapBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(FoodmapBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
