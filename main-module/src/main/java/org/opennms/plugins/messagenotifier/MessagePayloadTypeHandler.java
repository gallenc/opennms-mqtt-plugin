package org.opennms.plugins.messagenotifier;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class MessagePayloadTypeHandler {
	
	public final static String TEXT="TEXT";

	public final static String JSON="JSON";

	public final static String XML="XML";

	public final static String PROTOBUF="PROTOBUF";

	public final static List<String> supportedPayloadTypes= Arrays.asList(TEXT,JSON,XML,PROTOBUF);

	public static Object parsePayload(byte[] payload, String payloadType){
		if(payloadType==null) throw new IllegalArgumentException("payloadType must not be null");

		Object returnObject=null;

		switch (payloadType) {
		case TEXT :  returnObject = parseTextPayload(payload);
		break;
		case JSON :  returnObject = parseJsonPayload(payload);
		break;
		case XML:  returnObject = parseXmlPayload(payload);
		break;
		case PROTOBUF:  returnObject = parseProtobufPayload(payload);
		break;
		default: throw new IllegalArgumentException("unsupported payloadType:"+payloadType);
		}
		return returnObject;
	}
	
	public static String parseTextPayload(byte[] payload){
		try{
			String payloadString = new String(payload, "UTF8");
			return payloadString;
		} catch (Exception ex) {
			throw new RuntimeException("problem parsing payload to String", ex);
		}
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
