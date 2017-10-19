package org.opennms.plugins.mqttclient.test.manual;


import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class CreateTestDataTest {
	private static final Logger LOG = LoggerFactory.getLogger(CreateTestDataTest.class);

	// works with 2017-10-19 10:15:02.854888
	private static final String DEFAULT_DATE_TIME_FORMAT_PATTERN="yyyy-MM-dd HH:mm:ss.SSSSSS";
	
	private static final String LATITUDE = "50.9246217";
	private static final String LONGITUDE = "-1.374114";
	
	int MINUTE_INTERVAL_MS = 60 * 1000;

	public static final String jsonTestMessage="{"
			+ " \"time\": \""+ jsonTime(new Date())+ "\","
			+ " \"id\": \"monitorID\","
			+ " \"cityName\": \"Southampton\","
			+ " \"stationName\": \"Common#1\","
			+ " \"latitude\": "+LATITUDE+ ","
			+ " \"longitude\": "+LONGITUDE+ ","
			+ " \"averaging\": 0,"
			+ " \"PM1\": 10,"
			+ " \"PM25\": 100,"
			+ " \"PM10\": 1000"
			+ "}";
	
	int NO_VALUES=100;
	int MODULUS=3;

	@Test
	public void simpleTestData(){
		

		JSONArray jsonarray = new JSONArray();
				
		Date date = new Date();
		long timems = date.getTime();
		
		for( int n=NO_VALUES; n>1; n--){
			
			
			JSONObject jsonObject=null;
			try{
				jsonObject = parseJson(jsonTestMessage);
			} catch(Exception e){
				LOG.debug("problem creating json message", e);
			}
			
			// new data each 5 minute interval
			int offsetms = MINUTE_INTERVAL_MS * n;
			String timeStr = jsonTime(new Date(timems-offsetms));

			jsonObject.put("time", timeStr);
			
			String pm1=Double.toString(formula( n));
			String pm10=Double.toString(formula( n) *.1);
			String pm25=Double.toString(formula( n)*.25) ;
			jsonObject.put("PM1",pm1);
			jsonObject.put("PM10",pm10);
			jsonObject.put("PM25",pm25);
						
			//System.out.println(jsonObject.toJSONString());
			jsonarray.add(jsonObject);
			

		}
		System.out.println(jsonarray.toJSONString().replaceAll("},", "},\n"));
	}
	
	double formula(int n){
		double x = Math.sin(n /MODULUS);
		return x;
	}
	

	private JSONObject parseJson(String payloadString) throws ParseException{
		JSONObject jsonObject=null;
		JSONParser parser = new JSONParser();
		Object obj;
		obj = parser.parse(payloadString);
		jsonObject = (JSONObject) obj;
		return jsonObject;
	}


	public static String jsonTime(Date date){

		// default to local time offset
		ZoneOffset zoneOffset = OffsetDateTime.now().getOffset();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT_PATTERN);
		Instant instantFromDate = date.toInstant();
		LocalDateTime endLocalDateTime = LocalDateTime.ofInstant(instantFromDate,zoneOffset);
		String outputStr2 = endLocalDateTime.format(formatter);

		return outputStr2;

	}
	

}
