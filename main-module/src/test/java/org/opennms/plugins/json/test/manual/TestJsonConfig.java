package org.opennms.plugins.json.test.manual;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
//import java.text.ParseException;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.opennms.plugins.json.OnmsCollectionAttributeMap;
import org.opennms.plugins.json.OnmsAttributeJsonHandler;
import org.opennms.protocols.xml.config.XmlGroups;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestJsonConfig {
	private static final Logger LOG = LoggerFactory.getLogger(TestJsonConfig.class);

	String fileUri="src/test/resources/testJsonGroups.xml";

	String jsonUri="src/test/resources/testjsongroups.json";

	@Test
	public void testExtractJson() {
		LOG.info("start testExtractJson()");
		try {

			File pluginManagerFile = new File(fileUri);

			JAXBContext jaxbContext = JAXBContext.newInstance(XmlGroups.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

			XmlGroups xmlGroups = (XmlGroups) jaxbUnmarshaller.unmarshal(pluginManagerFile);

			LOG.info("test loaded historic data from file="+pluginManagerFile.getAbsolutePath());

			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			StringWriter stringWriter = new StringWriter();
			jaxbMarshaller.marshal(xmlGroups ,stringWriter);
			LOG.info("output string xmlgroups: \n"+stringWriter.toString());

			OnmsAttributeJsonHandler waj = new OnmsAttributeJsonHandler();

			JSONObject json=null;
			try {
				json = parseJson(jsonUri);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} 
			LOG.info("output string json: \n"+json.toJSONString());
			
			List<OnmsCollectionAttributeMap> attributeMapList = new ArrayList<OnmsCollectionAttributeMap>();

			try {
				waj.fillAttributeMap(attributeMapList , xmlGroups, json);
			} catch (java.text.ParseException e) {
				e.printStackTrace();
			}

			LOG.debug("attributeMap.size: "+attributeMapList.size());
			
			for( OnmsCollectionAttributeMap attributeMap: attributeMapList){
				for(String key: attributeMap.getAttributeMap().keySet()){
					LOG.debug("key:"+key + " Value:"+attributeMap.getAttributeMap().get(key).toString());
				}
			}
		



		} catch (JAXBException e) {
			throw new RuntimeException("Problem loading Plugin Manager Data",e);
		}
		LOG.info("end testExtractJson()");
	}

	@Test
	public void testParseTestMessages() {
		LOG.debug("start testParseTestMessages()");
		JSONObject jsonobj;
		try {

			jsonobj = parseJson(jsonUri);

			LOG.debug("testParseTestMessages:"+jsonobj.toJSONString());

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		LOG.debug("end testParseTestMessages)");
	}


	private JSONObject parseJson(String jsonUri) throws ParseException, FileNotFoundException, IOException {
		JSONObject jsonObject=null;
		JSONParser parser = new JSONParser();
		Object obj;
		obj = parser.parse((new FileReader(jsonUri)));
		jsonObject = (JSONObject) obj;
		return jsonObject;
	}

}
