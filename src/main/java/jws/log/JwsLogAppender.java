package jws.log;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

@Plugin(category = "Core", name = "JwsLogAppender", elementType = Appender.ELEMENT_TYPE)
public class JwsLogAppender extends AbstractAppender{
	
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock readLock = lock.readLock();
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	public final String INSERT_AMT_LOG = "INSERT INTO `AMT_LOG` (`LOG_LEVEL`, `THREAD_ID`, `SESSION_ID`, `USERID`, `CLASSNAME`, `MODULE`,"
			+ " `MESSAGE`, `EXCEPTION_MESSAGE`, `STACK_TRACE`, `REQUEST_MAPPING_URL`, `ENTRY_DATE`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	protected final String INSERT_ALERT_DASHBOARD= "INSERT INTO ALERT_DASHBOARD (OWNER_ID, TABLE_ID, TABLE_FIELD, TABLE_NAME, MODULE_NAME, DESCRIPTION, "
			+ "MAX_TIME, CREATED_DATE, STATUS, SUMMARY_STATUS, ACK_BY, SUMMARY_CREATED_DATE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

	@SuppressWarnings("deprecation")
	protected JwsLogAppender(String name, Filter filter, Layout<? extends Serializable> layout,
			boolean ignoreExceptions) {
		super(name, filter, layout, ignoreExceptions);
	}

	@PluginFactory
	public static JwsLogAppender createAppender(
			@PluginAttribute("name") final String name,
			@PluginAttribute("ignoreExceptions") final boolean ignore,
			@PluginElement("Filter") final Filter filter,
			@PluginElement("Layout") final Layout<? extends Serializable> layout){
		return new JwsLogAppender(name,filter,layout,ignore);
	}

	@Override
	public void append(LogEvent event) {
		this.readLock.lock();
		
		try{
			String stTos = new String();
			String expMessage = new String();
			if(event.getThrown() != null){
				StackTraceElement[] st = event.getThrown().getStackTrace();
				stTos = Arrays.toString(st);
				expMessage = event.getThrown().getMessage();
			}
			final Object[] obj = new Object[]{event.getLevel().name(), event.getThreadName()+"::"+event.getThreadId(), "", "", event.getLoggerName(), "JWS Log Extractor", 
					event.getMessage().getFormattedMessage(), expMessage, stTos, "", new java.sql.Timestamp(new java.util.Date().getTime())};
			KeyHolder keyHolder = new GeneratedKeyHolder();
			jdbcTemplate.update(new PreparedStatementCreator() {           
				@Override
				public PreparedStatement createPreparedStatement(Connection connection){
					PreparedStatement ps = null;
					try {
						ps = (PreparedStatement) connection.prepareStatement(INSERT_AMT_LOG, Statement.RETURN_GENERATED_KEYS );
						for(int i = 0; i < obj.length;i++){
							ps.setObject(i+1, obj[i]);
						}
					} catch (SQLException e) {
						System.out.println("SQL Exception while inserting into AMT_LOG :: "+Arrays.toString(obj)+"\n"+e.getMessage());
						e.printStackTrace();
					}
					return ps;
				}
			}, keyHolder);
			long id = keyHolder.getKey().longValue();
			if(event.getLevel().name().equals("ERROR") ||event.getLevel().name().equals("FATAL") ||event.getLevel().name().equals("WARN")){
				StringBuilder sb = new StringBuilder();
				sb.append("Level : "+event.getLevel().name()+"; ");
				sb.append("Class : "+event.getLoggerName()+"; ");
				sb.append("Message : "+event.getMessage().getFormattedMessage()+";");
				Object[] obj1 = new Object[]{111, id, "amtLogId", "AmtLog", "JWS Log Extractor", new String(sb),
						0, new java.sql.Timestamp(new java.util.Date().getTime()),event.getLevel().name(), "-", "ALERT_USER", new java.sql.Timestamp(new java.util.Date().getTime()) };
				try{
					id = jdbcTemplate.update(INSERT_ALERT_DASHBOARD, obj1);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			this.readLock.unlock();
		}

	}

}

