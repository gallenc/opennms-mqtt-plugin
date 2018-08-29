package org.opennms.plugins.messagehandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import jline.internal.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class MessagePayloadTypeHandler {
	private static final Logger LOG = LoggerFactory.getLogger(MessagePayloadTypeHandler.class);

	public final static String TEXT_CSV="TEXT_CSV";

	public final static String TEXT_CSV_HEADER="TEXT_CSV_HEADER";

	public final static String JSON="JSON";

	public final static String XML="XML";

	public final static String PROTOBUF="PROTOBUF";

	public final static List<String> supportedPayloadTypes= Arrays.asList(TEXT_CSV_HEADER,TEXT_CSV,JSON,XML,PROTOBUF);

	public static Object parsePayload(byte[] payload, String payloadType, String compression){
		if(payloadType==null) throw new IllegalArgumentException("payloadType must not be null");

		byte[] payload2 = payload;
		
		payload2= CompressionMethods.decompress(payload,compression);

		Object returnObject=null;

		switch (payloadType) {
		case TEXT_CSV :  returnObject = parseCsvPayload(payload2,false);
		break;
		case TEXT_CSV_HEADER :  returnObject = parseCsvPayload(payload2,true);
		break;
		case JSON :  returnObject = parseJsonPayload(payload2);
		break;
		case XML:  returnObject = parseXmlPayload(payload2);
		break;
		case PROTOBUF:  returnObject = parseProtobufPayload(payload2);
		break;
		default: throw new IllegalArgumentException("unsupported payloadType:"+payloadType);
		}
		return returnObject;
	}

	public static List<List<String>>  parseCsvPayload(byte[] payload, boolean header){
		try{
			String payloadString = new String(payload, "UTF8");
			String[] lines = payloadString.split("\\R+"); // split at line separator ignore empty lines

			if(header && lines.length>0)  lines = Arrays.copyOfRange(lines, 1, lines.length);

			List<List<String>> splitLines = new ArrayList<List<String>>();
			for(String line: lines){
				List<String> x = csvToList(line);
				splitLines.add(x);
			}

			return splitLines;

		} catch (Exception ex) {
			throw new RuntimeException("problem parsing payload as CSV String", ex);
		}
	}

	private static List<String> csvToList(String csvStr){
		final List<String> csvList;
		if (csvStr != null && !csvStr.trim().isEmpty()) {
			csvList = Arrays.asList(csvStr.split(",")).stream()
					.filter(h -> h != null && !h.trim().isEmpty())
					.map(h -> h.trim())
					.collect(Collectors.toList());
		} else csvList = new ArrayList<String>();
		return csvList;
	}

	public static JSONObject parseJsonPayload(byte[] payload){
		JSONObject jsonObject=null;
		JSONParser parser = new JSONParser();
		String payloadString=null;
		Object obj;
		try {
			payloadString = new String(payload, "UTF8");
			obj = parser.parse(new StringReader(payloadString));

			if (obj instanceof JSONObject) {
				jsonObject = (JSONObject) obj;
				return jsonObject;
			} else if (obj instanceof JSONArray) {
				// handle json starting with unnamed array e.g. [{ "id": "monitorID", "PM10": 1000 },{ "id": "monitorID2", "PM10": 1000 }]
				// creating a named array object to specifically parse
				// e.g. {array: [{ "id": "monitorID", "PM10": 1000 },{ "id": "monitorID2", "PM10": 1000 }]}
				JSONArray array = (JSONArray) obj;
				jsonObject = new JSONObject();
				jsonObject.put("array", array); 
				return jsonObject;
			} else throw new RuntimeException("unexpected type returned from jsonsimple parser:"+ obj.getClass());
		} catch (Exception ex) {
			throw new RuntimeException("problem parsing string to JSONObject:"+payloadString , ex);
		}
	}

	public static Document parseXmlPayload(byte[] payload){

		String payloadString = null;
		try {
			payloadString = new String(payload, "UTF8");
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setIgnoringComments(true);
			factory.setNamespaceAware(true);
			DocumentBuilder builder;

			builder = factory.newDocumentBuilder();

			InputStream stream = new ByteArrayInputStream(payloadString.getBytes(StandardCharsets.UTF_8.name()));
			Document doc = builder.parse(stream);

			// Ugly hack to deal with DOM & XPath 1.0's battle royale 
			// over handling namespaces without a prefix. 
			if(doc.getNamespaceURI() != null && doc.getPrefix() == null){
				factory.setNamespaceAware(false);
				builder = factory.newDocumentBuilder();
				stream = new ByteArrayInputStream(payloadString.getBytes(StandardCharsets.UTF_8.name()));
				doc = builder.parse(stream);
			}
			return doc;
		} catch (Exception ex) {
			throw new RuntimeException("problem parsing xml payload to document:"+payloadString , ex);
		}
	}

	public static Object parseProtobufPayload(byte[] payload){
		throw new UnsupportedOperationException("parseProtobufPayload not yet implimented");
	}


}
