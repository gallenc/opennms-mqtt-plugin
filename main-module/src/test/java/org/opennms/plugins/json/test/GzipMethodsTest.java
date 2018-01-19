package org.opennms.plugins.json.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.opennms.plugins.messagenotifier.GzipMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GzipMethodsTest {
	private static final Logger LOG = LoggerFactory.getLogger(GzipMethodsTest.class);
	

	@Test
	public void test1() throws IOException {
		LOG.debug("start GzipMethodsTest test1");
		
		String inputString = TestingSniffyKuraMessages.KURA_TYPED_JSON;
		
		// convert input inputString to byte array 
		byte[] payload = stringToByte(inputString);

		assertFalse(GzipMethods.isCompressed(payload));		
				
		byte[] compressedPayload = GzipMethods.compress(payload);
		
		assertTrue(GzipMethods.isCompressed(compressedPayload));
		
		byte[] decompressedPayload = GzipMethods.decompress(compressedPayload);

		String outputString = new String( decompressedPayload, "UTF8");
		
		assertEquals(inputString,outputString);
		
		LOG.debug("end GzipMethodsTest test1");	
		
	}
	
	public byte[] stringToByte(String inputString){
		byte[] payload;
		try {
			payload = inputString.getBytes(StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		return payload;
	}

}
