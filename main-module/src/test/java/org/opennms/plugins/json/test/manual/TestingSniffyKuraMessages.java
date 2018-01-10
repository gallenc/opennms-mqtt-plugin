/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2002-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/


package org.opennms.plugins.json.test.manual;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.Pointer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.opennms.plugins.mqttclient.test.manual.MQTTJsonTransmitterTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestingSniffyKuraMessages {
	private static final Logger LOG = LoggerFactory.getLogger(MQTTJsonTransmitterTest.class);

	@Test
	public void testParseTestMessages() {
		LOG.debug("start testParseTestMessages()");
		JSONObject jsonobj;
		try {
			jsonobj = parseJson(KURA_TYPED_JSON);
			LOG.debug("testParseTestMessages() KURA_TYPED_JSON JSON object:"+jsonobj.toJSONString() );
		} catch (ParseException e) {
			LOG.debug("failed to parse message:"+KURA_TYPED_JSON,e);		
			fail("failed to parse message:"+KURA_TYPED_JSON);
		}
		try {
			jsonobj = parseJson(KURA_SIMPLE_JSON);
			LOG.debug("testParseTestMessages() KURA_SIMPLE_JSON JSON object:"+jsonobj.toJSONString() );
		} catch (ParseException e) {
			LOG.debug("failed to parse message:"+KURA_SIMPLE_JSON,e);		
			fail("failed to parse message:"+KURA_SIMPLE_JSON);
		}
		try {
			jsonobj = parseJson(SNIFFY_TEST_MESSAGE);
			LOG.debug("testParseTestMessages() SNIFFY_TEST_MESSAGE JSON object:"+jsonobj.toJSONString() );
		} catch (ParseException e) {
			LOG.debug("failed to parse message:"+SNIFFY_TEST_MESSAGE,e);		
			fail("failed to parse message:"+SNIFFY_TEST_MESSAGE);
		}

		LOG.debug("end testParseTestMessages)");
	}


	private JSONObject parseJson(String payloadString) throws ParseException{
		JSONObject jsonObject=null;
		JSONParser parser = new JSONParser();
		Object obj;
		obj = parser.parse(payloadString);
		jsonObject = (JSONObject) obj;
		return jsonObject;
	}

	// TEST DATA - SEE Kura format https://github.com/eclipse/kapua/wiki/K-Payload-JSON-Format
	//	{
	//	    "sentOn" : 1491298822,
	//	    "position" : {
	//	    "latitude" : 45.234,
	//	    "longitude" : -7.3456,
	//	    "altitude" : 1.0,
	//	    "heading" : 5.4,
	//	    "precision" : 0.1,
	//	    "speed" : 23.5,
	//	    "timestamp" : 1191292288,
	//	    "satellites" : 3,
	//	    "status" : 2
	//	    },
	//	    "metrics": {
	//	    "code" : { "string" : "A23D44567Q" },
	//	    "distance" : { "double" : 0.26456E+4 },
	//	    "temperature" : { "float" : 27.5 },
	//	    "count" : { "int32" : 12354 },
	//	    "timestamp" : { "int64" : 23412334545 },
	//	    "enable" : { "bool" : true },
	//	    "rawBuffer" : { "bytes" : "cGlwcG8gcGx1dG8gcGFwZXJpbm8=" }
	//	    },
	//	    "body": "UGlwcG8sIHBsdXRvLCBwYXBlcmlubywgcXVpLCBxdW8gZSBxdWEu"
	//		}
	public static final String KURA_TYPED_JSON =
			"{ \n"
					+"    \"sentOn\" : 1491298822,  \n"
					+"    \"position\" : {  \n"
					+"    \"latitude\" : 45.234,  \n"
					+"    \"longitude\" : -7.3456,  \n"
					+"    \"altitude\" : 1.0,  \n"
					+"    \"heading\" : 5.4,  \n"
					+"    \"precision\" : 0.1,  \n"
					+"    \"speed\" : 23.5,  \n"
					+"    \"timestamp\" : 1191292288,  \n"
					+"    \"satellites\" : 3,  \n"
					+"    \"status\" : 2  \n"
					+"    },  \n"
					+"    \"metrics\": {  \n"
					+"    \"code\" : { \"string\" : \"A23D44567Q\" },  \n"
					+"    \"distance\" : { \"double\" : 0.26456E+4 },  \n"
					+"    \"temperature\" : { \"float\" : 27.5 },  \n"
					+"    \"count\" : { \"int32\" : 12354 },  \n"
					+"    \"timestamp\" : { \"int64\" : 23412334545 },  \n"
					+"    \"enable\" : { \"bool\" : true },  \n"
					+"    \"rawBuffer\" : { \"bytes\" : \"cGlwcG8gcGx1dG8gcGFwZXJpbm8=\" }  \n"
					+"    },  \n"
					+"    \"body\": \"UGlwcG8sIHBsdXRvLCBwYXBlcmlubywgcXVpLCBxdW8gZSBxdWEu\"  \n"
					+"  }  \n";

	//	{
	//	    "sentOn" : 1491298822,
	//	    "position" : {
	//	    "latitude" : 45.234,
	//	    "longitude" : -7.3456,
	//	    "altitude" : 1.0,
	//	    "heading" : 5.4,
	//	    "precision" : 0.1,
	//	    "speed" : 23.5,
	//	    "timestamp" : 1191292288,
	//	    "satellites" : 3,
	//	    "status" : 2
	//	    },
	//	    "metrics": {
	//	    "code" : "A23D44567Q",
	//	    "distance" : 0.26456E+4,
	//	    "temperature" : 27.5,
	//	    "count" : 12354,
	//	    "timestamp" : 23412334545,
	//	    "enable" : true,
	//	    "rawBuffer" : "cGlwcG8gcGx1dG8gcGFwZXJpbm8="
	//	    },
	//	    "body": "UGlwcG8sIHBsdXRvLCBwYXBlcmlubywgcXVpLCBxdW8gZSBxdWEu"
	//		}
	public static final String KURA_SIMPLE_JSON=
			"{  \n"
					+"    \"sentOn\" : 1491298822,  \n"
					+"    \"position\" : {  \n"
					+"    \"latitude\" : 45.234,  \n"
					+"    \"longitude\" : -7.3456,  \n"
					+"    \"altitude\" : 1.0,  \n"
					+"    \"heading\" : 5.4,  \n"
					+"    \"precision\" : 0.1,  \n"
					+"    \"speed\" : 23.5,  \n"
					+"    \"timestamp\" : 1191292288,  \n"
					+"    \"satellites\" : 3,  \n"
					+"    \"status\" : 2  \n"
					+"    },  \n"
					+"    \"metrics\": {  \n"
					+"    \"code\" : \"A23D44567Q\",  \n"
					+"    \"distance\" : 0.26456E+4,  \n"
					+"    \"temperature\" : 27.5,  \n"
					+"    \"count\" : 12354,  \n"
					+"    \"timestamp\" : 23412334545,  \n"
					+"    \"enable\" : true,  \n"
					+"    \"rawBuffer\" : \"cGlwcG8gcGx1dG8gcGFwZXJpbm8=\"  \n"
					+"    },  \n"
					+"    \"body\": \"UGlwcG8sIHBsdXRvLCBwYXBlcmlubywgcXVpLCBxdW8gZSBxdWEu\"  \n"
					+"  }  \n";


	public static final String SNIFFY_TEST_MESSAGE="{"
			+ " \"time\": \"2017-10-19 10:15:02.854888\","
			+ " \"id\": \"monitorID\","
			+ " \"cityName\": \"Southampton\","
			+ " \"stationName\": \"Common#1\","
			+ " \"latitude\": 0,"
			+ " \"longitude\": 0,"
			+ " \"averaging\": 0,"
			+ " \"PM1\": 10,"
			+ " \"PM25\": 100,"
			+ " \"PM10\": 1000"
			+ "}";

}
