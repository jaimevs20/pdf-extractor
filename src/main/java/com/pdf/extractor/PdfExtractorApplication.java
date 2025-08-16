package com.pdf.extractor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

@SpringBootApplication
public class PdfExtractorApplication {

	public static void main(String[] args) {
		try {
            RedisURI redisUri = RedisURI.Builder.redis(System.getenv("REDIS_HOST"))
                    .withPort(Integer.parseInt(System.getenv("REDIS_PORT")))
                    .withPassword(System.getenv("REDIS_PASSWORD").toCharArray())
                    .withSsl(true) // SSL obrigatório no Redis Cloud
                    .build();

            RedisClient client = RedisClient.create(redisUri);
            StatefulRedisConnection<String, String> connection = client.connect();
            RedisCommands<String, String> commands = connection.sync();
            commands.set("test", "hello");
            System.out.println("✅ Conectado ao Redis com sucesso. Valor test: " + commands.get("test"));
            connection.close();
            client.shutdown();
        } catch (Exception e) {
            System.err.println("❌ Falha ao conectar ao Redis: " + e.getMessage());
        }
		
		SpringApplication.run(PdfExtractorApplication.class, args);
	}

}
