package jws.log.notify;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class DataNotifier {
	
	private final Logger logger = LogManager.getLogger(this.getClass().getName());

	@Autowired
	private JmsTemplate jmsTemplate;
	
	@Autowired
	private Environment environment;
	
	public List<String> insertIntoQueue(List<JSONObject> list){
		List<String> idList = new ArrayList<String>();
		for(JSONObject json : list){
			try {
				jmsTemplate.convertAndSend(environment.getProperty("destinationQueue"), json.toString());
				String id = json.has("JWS_LOG") ? (json.getJSONObject("JWS_LOG").has("JWS_LOG_ID") ? json.getJSONObject("JWS_LOG").get("JWS_LOG_ID").toString() : "" ) : "";
				if(!id.equals(""))
					idList.add(id);
			} catch (JmsException e) {
				logger.error("JmsException in inserting into queue. Object :: {}", json, e);
			}catch (Exception e) {
				logger.error("Error in inserting into queue. Object :: {}", json, e);
			}
		}
		return idList;
	}
	
}
