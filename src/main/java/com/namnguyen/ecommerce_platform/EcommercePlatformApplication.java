package com.namnguyen.ecommerce_platform;

import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EcommercePlatformApplication {

	public static void main(String[] args) {

		SpringApplication.run(EcommercePlatformApplication.class, args);

//		String secret = Encoders.BASE64.encode(
//				Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256)
//						.getEncoded()
//		);
//
//		System.out.println(secret);
	}

}
