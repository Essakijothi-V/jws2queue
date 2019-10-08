package jws.log.extract;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jws.log.notify.DataNotifier;
import jws.log.processor.DataProcessor;

@Component
public class DataExtractor {
	
	private final Logger logger = LogManager.getLogger(this.getClass().getName());

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private DataProcessor processor;
	
	@Autowired
	private DataNotifier notifier;
	
	@Autowired
	private Environment environment;
	
	@Scheduled(initialDelayString="${schedulerTimeInMills}", fixedDelayString="${schedulerTimeInMills}")
	public void extract(){
		logger.info("Extracting data from database");
		jdbcTemplate.update("UPDATE "+environment.getProperty("readFromTable")+" SET FLAG = 'YTU' WHERE FLAG = 'N';");
		List<Map<String, Object>> queryForList = jdbcTemplate.queryForList("SELECT *, FROM_UNIXTIME(TIME_STAMP) AS TIME_STAMP_STR FROM `"+environment.getProperty("readFromTable")+"` WHERE FLAG = 'YTU';");
		if(!queryForList.isEmpty()){
			List<String> toBeinsertedIntoQueue = queryForList.stream().map(e -> e.get("JWS_LOG_ID").toString()).collect(Collectors.toList());
			List<JSONObject> processedData = processor.process(queryForList);
			List<String> insertedIntoQueue = notifier.insertIntoQueue(processedData);
			String insertedIdStr = toBeinsertedIntoQueue.toString().substring(1, insertedIntoQueue.toString().length()-1);
			toBeinsertedIntoQueue.removeAll(insertedIntoQueue);
			jdbcTemplate.batchUpdate("UPDATE "+environment.getProperty("readFromTable")+" SET FLAG = 'DELETE' WHERE FLAG = 'YTU' AND JWS_LOG_ID IN ("+insertedIdStr+") ;");
			if(!toBeinsertedIntoQueue.isEmpty()){
				logger.warn("Error in inserting data to queue: JWS_LOG_ID :: {}", toBeinsertedIntoQueue.toString());
			}
		}
	}
}
