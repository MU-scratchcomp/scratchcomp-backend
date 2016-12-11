package scratchreferee.scratchapi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

public final class ScratchProject {

	public final JSONObject payload;
	public final Map<String, byte[]> media = new HashMap<String, byte[]>();

	public ScratchProject(String sb2FileName) throws IOException, UnsupportedOperationException, IllegalArgumentException {
		ZipFile sb2File = new ZipFile(sb2FileName);
		try {
			Enumeration<? extends ZipEntry> sb2FileIterator = sb2File.entries();
			JSONObject payload = null;
			
			while (sb2FileIterator.hasMoreElements()) {
				ZipEntry entry = sb2FileIterator.nextElement();
				String fileName = entry.getName();
				
				if (fileName.equals("project.json")) {
					InputStream payloadStream = sb2File.getInputStream(entry);
					payload = new JSONObject(IOUtils.toString(payloadStream, Charset.defaultCharset()));
					payloadStream.close();
				} else {
					InputStream dataStream = sb2File.getInputStream(entry);
					byte[] data = IOUtils.toByteArray(dataStream);
					dataStream.close();
					try {
						String mediaName = Hex.encodeHexString(MessageDigest.getInstance("MD5").digest(data)).toLowerCase();
						mediaName += fileName.substring(fileName.lastIndexOf('.'));
						media.put(mediaName, data);
					} catch (NoSuchAlgorithmException e) {
						throw new UnsupportedOperationException(
								"No Java Security Provider supporting the MD5 hash algorithm could be found (MD5 is required to load Scratch project media)",
								e);
					}
				}
			}
			
			if (payload == null)
				throw new IllegalArgumentException("Specified sb2 file malformed - no project payload found (no project.json)");
			else
				this.payload = payload;
		} finally {
			sb2File.close();
		}
	}
}
