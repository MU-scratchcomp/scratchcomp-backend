package scratchreferee.scratchapi;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONObject;

public final class ScratchNetworkUtil {

	public static final class HttpResponse {
		public final Map<String, List<String>> headerFields;
		public final String body;
		
		private HttpResponse(Map<String, List<String>> responseFields, String responseBody) throws IOException {
			headerFields = responseFields;
			body = responseBody;
		}
		
		public final String headerFieldsToString() {
			StringBuffer stringBuffer = new StringBuffer();
			for (Entry<String, List<String>> headerField : headerFields.entrySet()) {
				stringBuffer.append(headerField.getKey() + ": ");
				for (String fieldValue : headerField.getValue())
					stringBuffer.append(fieldValue + " ");
				stringBuffer.append("\n");
			}
			return stringBuffer.toString();
		}
		
		public final JSONObject parseBody() {
			String body = this.body;
			if (body.startsWith("["))
				body = body.substring(1, body.length() - 1);
			return new JSONObject(body);
		}
	}
	
	public static final HttpResponse execute(String method, String hostname, String path, Map<String, String> headers,
			String sessionId, String body) throws IOException {
		return execute(method, hostname, path, headers, sessionId, body.getBytes());
	}
	
	public static final HttpResponse execute(String method, String hostname, String path, Map<String, String> headers,
			String sessionId, byte[] body) throws IOException {
		if (hostname == null)
			hostname = "scratch.mit.edu";
		if (path == null)
			path = "/";
		URL url = new URL("https://" + hostname + path);
		HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

		if (method == null)
			method = "GET";
		conn.setRequestMethod(method);

		Map<String, String> allHeaders = new HashMap<String, String>();
		allHeaders.put("Host", hostname);
		allHeaders.put("Referer", "https://scratch.mit.edu");
		allHeaders.put("Cookie", "scratchcsrftoken=a; scratchlanguage=en;");
		allHeaders.put("X-CSRFToken", "a");
		if (headers != null)
			for (Entry<String, String> entry : headers.entrySet())
				allHeaders.put(entry.getKey(), entry.getValue());
		if (body != null)
			allHeaders.put("Content-Length", Integer.toString(body.length));
		if (sessionId != null)
			allHeaders.put("Cookie", allHeaders.get("Cookie") + "scratchsessionsid=" + sessionId + ";");

		for (Entry<String, String> header : allHeaders.entrySet())
			conn.setRequestProperty(header.getKey(), header.getValue());
		
		conn.setDoOutput(true);
		conn.setDoInput(true);
		
		DataOutputStream connOut = new DataOutputStream(conn.getOutputStream());
		connOut.write(body);
		connOut.flush();
		connOut.close();
		
		BufferedReader connIn = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		StringBuffer responseBodyBuffer = new StringBuffer();
		String nextLine;
		while ((nextLine = connIn.readLine()) != null)
			responseBodyBuffer.append(nextLine);
		connIn.close();
		
		conn.disconnect();
				
		return new HttpResponse(conn.getHeaderFields(), responseBodyBuffer.toString());
	}

}
