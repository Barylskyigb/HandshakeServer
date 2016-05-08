package HandshakeServer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

/**
 * 1. Write to file ids needed to scan
 * 2. Use phantomjs script
 * 3. Write info to db from people/ folder
 */

@SpringBootApplication
public class Main  {

    public static void main(String[] args) throws IOException {
        SpringApplication.run(Main.class);
    }
}