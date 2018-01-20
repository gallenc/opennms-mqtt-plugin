package org.opennms.plugins.messagenotifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompressionMethods {
	private static final Logger LOG = LoggerFactory.getLogger(CompressionMethods.class);

	public static final String GZIP = "GZIP";

	public static final String AUTOMATIC_GZIP = "AUTOMATIC_GZIP";

	public static final String UNCOMPRESSED="UNCOMPRESSED";

	public static final List<String> availableCompressionTypes = Arrays.asList(GZIP,AUTOMATIC_GZIP,UNCOMPRESSED);

	public static boolean isCompressed(byte[] bytes) throws IOException {
		if (bytes == null || bytes.length < 2) {
			return false;
		} else {
			return bytes[0] == (byte) GZIPInputStream.GZIP_MAGIC
					&& bytes[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8);
		}
	}

	public static byte[] decompress(byte[] source, String compressionType){
		try {
			if(compressionType==null || UNCOMPRESSED.equals(compressionType)){
				return source;
			} else if (AUTOMATIC_GZIP.equals(compressionType)){
				if( isCompressed(source)){
					return decompressGzip(source);
				} else return source;
			} else if (GZIP.equals(compressionType)) {
				return decompressGzip(source);
			} else {
				LOG.warn("Cannot decompress payload trying to process as though uncompressed. Unknown compression setting:"+compressionType);
				return source;
			}
		}
		catch (IOException ex){
			LOG.warn("Cannot decompress payload trying to process as though uncompressed.");
			return source;
		}
	}


	public static byte[] decompressGzip(byte[] source) throws IOException {
		ByteArrayOutputStream byteArrayOutstream = new ByteArrayOutputStream();
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(source);
		GZIPInputStream gzipInputStream = null;

		try {
			gzipInputStream = new GZIPInputStream(byteArrayInputStream);

			int n;
			final int MAX_BUF = 1024;
			byte[] buf = new byte[MAX_BUF];
			while ((n = gzipInputStream.read(buf, 0, MAX_BUF)) != -1) {
				byteArrayOutstream.write(buf, 0, n);
			}
		} catch (IOException e) {
			throw e;
		} finally {
			if (gzipInputStream != null) {
				try {
					gzipInputStream.close();
				} catch (IOException e) {
					// Ignore
				}
			}

			try {
				byteArrayOutstream.close();
			} catch (IOException e) {
				// Ignore
			}
		}

		return byteArrayOutstream.toByteArray();
	}

	public static byte[] compressGzip(byte[] source) throws IOException {

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		GZIPOutputStream gzipOutputStream = null;
		try {
			gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
			gzipOutputStream.write(source);
		} catch (IOException e) {
			throw e;
		} finally {
			if (gzipOutputStream != null) {
				try {
					gzipOutputStream.close();
				} catch (IOException e) {
					// Ignore
				}
			}
		}
		return byteArrayOutputStream.toByteArray();
	}
}
