package jws.log.listener;

import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.annotation.JmsListenerConfigurer;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpointRegistrar;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.config.SimpleJmsListenerEndpoint;

@Configuration
public class JwsMultipleListener implements JmsListenerConfigurer {

	private final Logger logger = LogManager.getLogger(this.getClass().getName());
	
	@Value("${destinationQueues}")
	private String[] destinationQueues;

	@Value("${concurrency}")
	private String concurrency;
	
	@SuppressWarnings("rawtypes")
	@Autowired
	private JmsListenerContainerFactory jmsListenerContainerFactory;
	
	@Autowired
	private JmsListenerEndpointRegistry jmsListenerEndpointRegistry;

	@Autowired
	private Environment environment;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Override
	public void configureJmsListeners(JmsListenerEndpointRegistrar registrar) {
		registrar.setEndpointRegistry(jmsListenerEndpointRegistry);
		for(int i = 0; i < destinationQueues.length; i++){
			logger.info("Endpoint :: "+destinationQueues[i]+" initializing");
			try {
				SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
				endpoint.setId(String.valueOf(Math.random()));
				endpoint.setConcurrency(concurrency);
				endpoint.setDestination(destinationQueues[i]);
				endpoint.setMessageListener(message -> {
					try {
						TextMessage ob = (TextMessage) message;
						insertDataIntoDSMonitor(environment.getProperty("writeToTable"), new ArrayList<Object[]>(){
							/**
							 * 
							 */
							private static final long serialVersionUID = 7227706638684323949L;

							{
								add(new Object[]{ob.getText(), Thread.currentThread().getName()});
							}}, "INSERT INTO `"+environment.getProperty("writeToTable")+"` (`LOG`, `SRC`) VALUES (?, ?)");
					} catch (JMSException e) {
						e.printStackTrace();
					}
				});
				registrar.registerEndpoint(endpoint, jmsListenerContainerFactory);
			} catch (Exception e) {
				logger.error("Exception listening. "+e.getMessage(), e);
			}
		}
	}

	private void insertDataIntoDSMonitor(String table, List<Object[]> list, String insertquery) {

		try{
			if(!list.isEmpty()){
				if(jdbcTemplate != null){
					jdbcTemplate.batchUpdate(insertquery, list);
				}
			}
		}catch(Exception e){
			logger.error("While inserting into "+table, e);
		}

	}

}
