package org.opennms.plugins.payload.test;

import static org.junit.Assert.*;

import org.junit.Test;
import org.eclipse.paho.client.mqttv3.MqttTopic;

/**
 * This tests if the topic can match a given filter
 * @author admin
 *
 */
public class PahoTopicParserTest {
	
	@Test
	public void testTopicFilter() {

		// These Tests are similar to Mosquette 0.11 tests of Topic
		// see https://github.com/andsel/moquette/blob/34a2aca45ec3cc5a79daebdc728c6f1d910db9c4/broker/src/test/java/io/moquette/spi/impl/subscriptions/TopicTest.java
		// Note that not all matches are the same between Eclipse Mosquette and Eclipse Paho 1.1.1 
		// Eclipse paho 1.2 also changes how MqttTopic.isMatched works
		// These tests illustrate how you may expect topic subscription based upon Eclipse Paho 1.1.1 to work in the OpenNMS.

		//          MqttTopic.isMatched(topicFilter, topicName);
		assertTrue( MqttTopic.isMatched("+", "finance"));
		assertTrue( MqttTopic.isMatched("finance/+", "finance/stock"));
		assertFalse( MqttTopic.isMatched("finance/+", "finance"));
		assertTrue( MqttTopic.isMatched("/+", "/finance"));
		assertFalse( MqttTopic.isMatched("+", "/finance"));
		
		// difference Mosquette Eclipse Paho 1.1.1 
		// assertTrue( MqttTopic.isMatched("+/+", "/finance"));
		assertTrue( MqttTopic.isMatched("/finance/+/ibm", "/finance/stock/ibm"));
		
		// difference Mosquette Eclipse Paho 1.1.1 
		// assertTrue( MqttTopic.isMatched("+/+", "/"));
		assertTrue( MqttTopic.isMatched("sport/", "sport/"));
		assertTrue( MqttTopic.isMatched("/finance/stock", "/finance/stock"));
		assertFalse( MqttTopic.isMatched("+", "/finance/stock"));

		assertTrue( MqttTopic.isMatched("foo/bar" , "foo/bar") );
		assertTrue( MqttTopic.isMatched("foo/+" , "foo/bar") );
		assertTrue( MqttTopic.isMatched("foo/+/baz" , "foo/bar/baz") );
		assertTrue( MqttTopic.isMatched("foo/+/#" , "foo/bar/baz") );
		assertTrue( MqttTopic.isMatched("#" , "foo/bar/baz") );
		assertFalse( MqttTopic.isMatched("foo/bar" , "foo") );
		assertFalse( MqttTopic.isMatched("foo/+" , "foo/bar/baz") );
		assertFalse( MqttTopic.isMatched("foo/+/baz" , "foo/bar/bar") );
		assertFalse( MqttTopic.isMatched("foo/+/#" , "fo2/bar/baz") );
		
		// difference Mosquette Eclipse Paho 1.1.1 
		// assertTrue( MqttTopic.isMatched("#" , "/foo/bar") );
		assertTrue( MqttTopic.isMatched("/#" , "/foo/bar") );
		assertFalse( MqttTopic.isMatched("/#" , "foo/bar") );
		assertTrue( MqttTopic.isMatched("foo//bar" , "foo//bar") );
		assertTrue( MqttTopic.isMatched("foo//+" , "foo//bar") );
		
		// difference Mosquette Eclipse Paho 1.1.1 
		// assertTrue( MqttTopic.isMatched("foo/+/+/baz" , "foo///baz") );
		// assertTrue( MqttTopic.isMatched("foo/bar/+" , "foo/bar/") );

	}

}
