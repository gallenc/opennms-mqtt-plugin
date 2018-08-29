package org.opennms.plugins.payload.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.opennms.plugins.messagehandler.CompressionMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DecompressionMethodsTest {
	private static final Logger LOG = LoggerFactory.getLogger(DecompressionMethodsTest.class);
	

	@Test
	public void test1() throws IOException {
		LOG.debug("start DecompressionMethodsTest test1");
		
		String inputString = TestingSniffyKuraMessages.KURA_TYPED_JSON;
		
		// convert input inputString to byte array 
		byte[] payload = stringToByte(inputString);

		assertFalse(CompressionMethods.isCompressed(payload));		
				
		byte[] compressedPayload = CompressionMethods.compressGzip(payload);
		
		assertTrue(CompressionMethods.isCompressed(compressedPayload));
		
		byte[] decompressedPayload = CompressionMethods.decompressGzip(compressedPayload);

		String outputString = new String( decompressedPayload, "UTF8");
		
		assertEquals(inputString,outputString);
		
		LOG.debug("end DecompressionMethodsTest test1");	
		
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
