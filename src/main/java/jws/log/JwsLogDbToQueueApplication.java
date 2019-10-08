package jws.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.scheduling.annotation.EnableScheduling;

import jws.log.extract.DataExtractor;

@SpringBootApplication
@EnableScheduling
@EnableJms
public class JwsLogDbToQueueApplication {
	
	private static final Logger logger = LogManager.getLogger(JwsLogDbToQueueApplication.class);

	public static void main(String[] args) {
		logger.info("JwsLogDbToQueue started");
		ApplicationContext context = SpringApplication.run(JwsLogDbToQueueApplication.class, args);
		DataExtractor bean = context.getBean(DataExtractor.class);
		bean.extract();
	}

}
