package jws.log.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class DataProcessor {
	
	private final Logger logger = LogManager.getLogger(this.getClass().getName());

	public List<JSONObject> process(List<Map<String, Object>> queryForList) {
		logger.info("Data processing");
		List<JSONObject> list = new ArrayList<JSONObject>();
		for(Map<String, Object> map : queryForList){
			list.add(toJson(map));
		}
		return list;
	}
	
	public JSONObject toJson(Map<String, Object> inMap){
		Map<String, Object> map = new HashMap<String, Object>();
		Map<String, Object> jmsLog = new HashMap<String, Object>();
		map.put("LOG_SOURCE_CATEGORY", "JWS_LOG");
		map.put("SERVER_GROUP_NAME", "");
		map.put("NODE_NAME", "");
		map.put("OWNER_ID", "");
		map.put("SESSION_ID", "");
		map.put("ENTRY_DATETIME", getValueFromMapByKey(inMap, "TIME_STAMP_STR"));
		map.put("SOURCE_REQUEST_ID", getValueFromHeader(inMap, "HEADER", "IN_HEADER", "'SOURCE_REQUEST_ID'"));
		map.put("SOURCE_REQUEST_TYPE", getValueFromHeader(inMap, "HEADER", "IN_HEADER", "'SOURCE_REQUEST_TYPE'"));
		map.put("REQUEST_SEQUENCE_ID", getValueFromHeader(inMap, "HEADER", "IN_HEADER", "'REQUEST_SEQUENCE_ID'"));
		map.put("RUN_ENVIRONMENT", "");
		map.put("RUN_MODE", "");
		map.put("LOB_RUNTIME_SYSTEM", "");
		map.put("MESSAGE_LEVEL", "");
		map.put("MESSAGE", "");

		jmsLog.put("JWS_LOG_ID", getValueFromMapByKey(inMap, "JWS_LOG_ID"));
		jmsLog.put("AGENT", getValueFromMapByKey(inMap, "AGENT"));
		jmsLog.put("RESPONSE_SIZE_IN_BYTES", Long.parseLong(Optional.ofNullable(inMap.get("BYTES_SENT").toString()).orElse("0")));
		jmsLog.put("REFERER", getValueFromMapByKey(inMap, "REFERER"));
		jmsLog.put("COOKIES", getValueFromMapByKey(inMap, "COOKIE"));
		jmsLog.put("REMOTE_HOST_IP", getValueFromMapByKey(inMap, "REMOTE_HOST"));
		jmsLog.put("REQUEST_DURATION_MILLIS", Integer.parseInt(Optional.ofNullable(inMap.get("REQUEST_DURATION_MILLIS").toString()).orElse("")));
		jmsLog.put("REQUEST_DURATION_SEC", Integer.parseInt(Optional.ofNullable(inMap.get("REQUEST_DURATION_SEC").toString()).orElse("")));
		jmsLog.put("REQUEST_LINE", getValueFromMapByKey(inMap, "REQUEST_LINE"));
		jmsLog.put("VIRTUAL_HOST", getValueFromMapByKey(inMap, "VIRTUAL_HOST"));
		try {
			jmsLog.put("HEADER", new JSONObject(getValueFromMapByKey(inMap, "HEADER").toString().replaceAll("'", "")));
		} catch (JSONException e) {
			logger.error("Error in getting header :: {}", inMap, e);
		}
		
		map.put("NEXTGEN_LOG", new HashMap<String, Object>());
		map.put("CURRENTGEN_LOG", new HashMap<String, Object>());
		map.put("EAP_SERVER_LOG", new HashMap<String, Object>());
		map.put("JWS_LOG", jmsLog);
		
		return new JSONObject(map);
	}
	
	private Object getValueFromMapByKey(Map<String, Object> map, String key){
		if(map.containsKey(key))
			return Optional.ofNullable(map.get(key)).orElse(null);
		return "";
	}
	
	private Object getValueFromHeader(Map<String, Object> object, String... key) {
		String ret = new String();
		try {
			JSONObject json = new JSONObject(object);
			Object ob = new Object();
			for(int i = 0; i < key.length; i++){
				if(json.has(key[i])){
					ob = json.get(key[i]);
					try {
						json = new JSONObject(ob.toString());
					} catch (Exception e) {}
					if(i == key.length-1)
						ret = ob.toString().replaceAll("'", "");
				}
				
			}
		} catch (JSONException e) {
			logger.error("Error in getting value from header Object :: {}, Keys :: {}", object, Arrays.toString(key), e);
		}
		return ret;
	}
}
