package org.opennms.plugins.messagenotifier.datanotifier;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.opennms.netmgt.collection.api.AttributeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigDao {
	private static final Logger LOG = LoggerFactory.getLogger(ConfigDao.class);
	
	// works with 2017-10-19 10:15:02.854888
	private static final String DEFAULT_DATE_TIME_FORMAT_PATTERN="yyyy-MM-dd HH:mm:ss.SSSSSS";

	private final List<String> _DEFAULT_RRAS = Arrays.asList(
			// Use the default list of RRAs we provide in our stock configuration files
			"RRA:AVERAGE:0.5:1:2016",
			"RRA:AVERAGE:0.5:12:1488",
			"RRA:AVERAGE:0.5:288:366",
			"RRA:MAX:0.5:288:366",
			"RRA:MIN:0.5:288:366");

	private final int DEFAULT_INTERVAL_IN_SECONDS = 300;

	private Map<String,AttributeType> dataDefinition= new LinkedHashMap<String,AttributeType>();

	private String m_dateTimeFormatPattern=DEFAULT_DATE_TIME_FORMAT_PATTERN;
	
	private String timeZoneOffset= null;

	private String foreignSource;

	private String foreignIdKey;

	private String timeStampKey;

	private String group;

	private Integer intervalInSeconds = DEFAULT_INTERVAL_IN_SECONDS;

	private List<String> rras=_DEFAULT_RRAS;

	public List<String> getRras() {
		return rras;
	}

	public void setRras(List<String> rras) {
		if(rras==null || !(rras.size()>0)) throw new RuntimeException("rras definition must not be null or empty");
		this.rras = rras;
	}

	public Integer getIntervalInSeconds() {
		return intervalInSeconds;
	}
	
	public void setIntervalInSecondsStr(String intervalInSeconds) {
		this.intervalInSeconds = Integer.parseInt(intervalInSeconds);
	}

	public Map<String, AttributeType> getDataDefinition() {
		return dataDefinition;
	}

	public void setDataDefinition(Map<String, AttributeType> dataDefinition) {
		this.dataDefinition = dataDefinition;
	}

	public String getForeignSource() {
		return foreignSource;
	}

	public void setForeignSource(String foreignSource) {
		this.foreignSource = foreignSource;
	}

	public String getForeignIdKey() {
		return foreignIdKey;
	}

	public void setForeignIdKey(String foreignIdKey) {
		this.foreignIdKey = foreignIdKey;
	}

	public String getTimeStampKey() {
		return timeStampKey;
	}

	public void setTimeStampKey(String timeStampKey) {
		this.timeStampKey = timeStampKey;
	}

	public void setGroup(String group) {
		this.group = group;
	}
	
	public String getGroup() {
		return group;
	}
	
	
	public String getDateTimeFormatPattern() {
		return m_dateTimeFormatPattern;
	}

	public void setDateTimeFormatPattern(String dateTimeFormatPattern) {
		if(dateTimeFormatPattern==null || "".equals(dateTimeFormatPattern.trim())) {
			this.m_dateTimeFormatPattern =DEFAULT_DATE_TIME_FORMAT_PATTERN;
			LOG.info("supplied dateTimeFormatPattern empty using default:"+this.m_dateTimeFormatPattern);
		}
		try{
			new SimpleDateFormat(dateTimeFormatPattern);
		} catch (Exception e){
			LOG.error("using default "+DEFAULT_DATE_TIME_FORMAT_PATTERN
					+ " because cannot parse supplied dateTimeFormatPattern: "+dateTimeFormatPattern);
		}
		this.m_dateTimeFormatPattern = dateTimeFormatPattern;
	}

	// methods to convert comma separated lists into List<string>
	
	public void setRrasProperty(String rrasStr) {
		List<String> rrasList = csvPropertyToList(rrasStr);
		setRras(rrasList);
	}


	public void setCounterKeysProperty(String counterKeysStr) {
		List<String> counterKeys = csvPropertyToList(counterKeysStr);
		for (String key:counterKeys){
			dataDefinition.put(key, AttributeType.COUNTER);
		}
	}

	public void setGuageKeysProperty(String guageKeysStr) {
		List<String> guageKeys = csvPropertyToList(guageKeysStr);
		for (String key:guageKeys){
			dataDefinition.put(key, AttributeType.GAUGE);
		}
	}

	public void setStringKeysProperty(String stringKeysStr) {
		List<String> stringKeysKeys = csvPropertyToList(stringKeysStr);
			for (String key:stringKeysKeys){
				dataDefinition.put(key, AttributeType.STRING);
			}
	}

	private List<String> csvPropertyToList(String csvStr){
		final List<String> csvList;
		if (csvStr != null && !csvStr.trim().isEmpty()) {
			csvList = Arrays.asList(csvStr.split(",")).stream()
					.filter(h -> h != null && !h.trim().isEmpty())
					.map(h -> h.trim())
					.collect(Collectors.toList());
		} else csvList = new ArrayList<String>();
		return csvList;
	}


	@Override
	public String toString() {
		StringBuffer msg = new StringBuffer("ConfigDao [foreignSource=" + foreignSource + ", foreignIdKey="
				+ foreignIdKey + ", timeStampKey=" + timeStampKey + ", group="
				+ group + ", intervalInSeconds=" + intervalInSeconds+", m_dateTimeFormatPattern=" + m_dateTimeFormatPattern
				+", timeZoneOffset=" + timeZoneOffset);
		msg.append("\n dataDefinition");
		for (String attributeName :dataDefinition.keySet()){
			msg.append("   attributeName:"+attributeName+" dataType:"+dataDefinition.get(attributeName).getName()+"\n");
		}
		msg.append("\n rras");
		for (String rra :rras){
			msg.append("   "+rra+"\n");
		}
		msg.append("]");
		return msg.toString();
		
	}

	public String getTimeZoneOffset() {
		return timeZoneOffset;
	}

	public void setTimeZoneOffset(String timeZoneOffset) {
		this.timeZoneOffset = timeZoneOffset;
	}


}
