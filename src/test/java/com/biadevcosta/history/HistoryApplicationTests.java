package com.biadevcosta.history;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Full-context test needs MySQL + Kafka (Docker) and public.pem; replaced by HistoryIntegrationTest in step 10")
class HistoryApplicationTests {

	@Test
	void contextLoads() {
	}

}
