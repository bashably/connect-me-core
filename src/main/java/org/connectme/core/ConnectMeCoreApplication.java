package org.connectme.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class ConnectMeCoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConnectMeCoreApplication.class, args);
	}
}
