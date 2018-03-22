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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Operations regarding interaction with the Scratch web platform,
 * scratch.mit.edu.
 * 
 * This class is intended to only be accessed statically, and is thread safe.
 * 
 * @author Charlie Morley
 * @version 1.1.0, 2016-12-11
 * @since 1.0.0, 2016-12-10
 */
final class ScratchNetworkUtil {

	/**
	 * Summary of the Scratch web platform's response to an HTTP request by this
	 * API.
	 * 
	 * @author Charlie Morley
	 * @version 1.0.0, 2016-12-10
	 */
	public static final class HttpResponse {

		/**
		 * (from {@link java.net.URLConnection#getHeaderFields()
		 * URLConnection.getHeaderFields()}) An unmodifiable Map of the HTTP
		 * header fields. The Map keys are Strings that represent the response
		 * header field names. Each Map value is an unmodifiable List of Strings
		 * that represents the corresponding field values.
		 */
		public final Map<String, List<String>> headerFields;

		/**
		 * The data sent in this response, if any. If there was no data
		 * following the response header, this field contains the empty string.
		 */
		public final String body;

		/**
		 * Creates a new response containing the given header fields and body
		 * data.
		 * 
		 * @param responseFields
		 *            an unmodifiable Map binding the response header field
		 *            names to unmodifiable lists of the corresponding field
		 *            values
		 * @param responseBody
		 *            the data following the header of this response
		 */
		private HttpResponse(Map<String, List<String>> responseFields, String responseBody) {
			headerFields = responseFields;
			body = responseBody;
		}

		/**
		 * Converts this response's header fields to a readable format, for
		 * technical debugging or reporting purposes. The header fields appear,
		 * one on each line, with the header field name; followed by a colon;
		 * followed by the values of the header field, space-separated. For
		 * example:
		 * <p>
		 * {@code null: HTTP/1.1 200 OK}<br>
		 * {@code Host: www.example.com}<br>
		 * {@code Content-Length: 2000}
		 * </p>
		 * 
		 * The header fields do not appear in any particular order.
		 * 
		 * @return a string representation of this response's
		 *         {@link #headerFields headerFields}
		 */
		public String headerFieldsToString() {
			StringBuffer stringBuffer = new StringBuffer();
			for (Entry<String, List<String>> headerField : headerFields.entrySet()) {
				stringBuffer.append(headerField.getKey() + ": ");
				for (String fieldValue : headerField.getValue())
					stringBuffer.append(fieldValue + " ");
				stringBuffer.append("\n");
			}
			return stringBuffer.toString();
		}

		/**
		 * Attempts to parse this response's data as a JSON object.
		 * 
		 * @return a {@code JSONObject} representation of this response's
		 *         {@link #body body}
		 * @throws JSONException
		 *             if this response's data could not be parsed
		 * @see org.json.JSONObject#JSONObject(String) JSONObject(String)
		 */
		public JSONObject parseBody() throws JSONException {
			String body = this.body;
			if (body.startsWith("["))
				body = body.substring(1, body.length() - 1);
			return new JSONObject(body);
		}
	}

	/**
	 * Connects to a Scratch server and sends an HTTP request with the given
	 * details, and returns the response. <br>
	 * This method is simply a convenience overload of
	 * {@link ScratchNetworkUtil#execute(String, String, String, Map, String, byte[])
	 * ScratchNetworkUtil.execute(..., byte[])} - this method calls that method
	 * with {@code body} equal to this method's {@code body.}
	 * {@link String#getBytes() getBytes()}.
	 * 
	 * @param method
	 *            the HTTP request method; e.g., GET, POST, or HEAD
	 * @param hostname
	 *            the host to send the request to
	 * @param path
	 *            the path of the request to be sent - the end part of the
	 *            connection URL
	 * @param headers
	 *            a Map binding any additional header field names desired in the
	 *            HTTP request to their corresponding values
	 * @param sessionId
	 *            a Scratch session ID to be included in the "Cookie" header
	 *            field, used to maintain a user session across various requests
	 *            to the Scratch website
	 * @param body
	 *            the body data of the request to be sent (following the request
	 *            headers)
	 * @return an {@code HttpResponse} object containing the details and data of
	 *         the request response from the server
	 * @throws IOException
	 *             if an I/O exception occurs.
	 * @since 1.1.0
	 * @see ScratchNetworkUtil#execute(String, String, String, Map, String,
	 *      byte[])
	 */
	public static final HttpResponse execute(String method, String hostname, String path, Map<String, String> headers,
			String sessionId, String body) throws IOException {
		return execute(method, hostname, path, headers, sessionId, body.getBytes());
	}

	/**
	 * Connects to a Scratch server and sends an HTTP request with the given
	 * details, and returns the response. <br>
	 * Besides the details specified as input parameters, the HTTP request sent
	 * by this method has the following header fields:
	 * <p>
	 * <ul>
	 * <li>{@code Referer: https://scratch.mit.edu}
	 * <li>{@code Cookie: scratchcsrftoken=a; scratchlanguage=en;}
	 * <li>{@code X-CRSFToken: a}
	 * </ul>
	 * </p>
	 * 
	 * @param method
	 *            the HTTP request method; e.g., GET, POST, or HEAD. Default
	 *            value (if null): GET
	 * @param hostname
	 *            the host to send the request to. This becomes the first part
	 *            of the connection URL and also the value for the "Host" header
	 *            field. Default value (if null): {@code scratch.mit.edu}
	 * @param path
	 *            the path of the request to be sent - the end part of the
	 *            connection URL. Default value (if null): {@code /}
	 * @param headers
	 *            a Map binding any additional header field names desired in the
	 *            HTTP request to their corresponding values
	 * @param sessionId
	 *            a Scratch session ID to be included in the "Cookie" header
	 *            field, used to maintain a user session across various requests
	 *            to the Scratch website. A null value for this parameter
	 *            usually results in the host server providing a new session ID
	 *            (in the "Set-Cookie" header field of the returned response)
	 * @param body
	 *            the body data of the request to be sent (following the request
	 *            headers). A non-null value for this parameter results in the
	 *            "Content-Length" header field being added to the request with
	 *            the length of the supplied array as its value. A null value
	 *            indicates no data to be sent after the headers
	 * @return an {@code HttpResponse} object containing the details and data of
	 *         the request response from the server
	 * @throws IOException
	 *             if an I/O exception occurs.
	 * @since 1.0.0
	 * @see URL#openConnection()
	 * @see HttpsURLConnection
	 */
	public static HttpResponse execute(String method, String hostname, String path, Map<String, String> headers,
			String sessionId, byte[] body) throws IOException {
		/*
		 * Sets up connection to the host
		 */
		if (hostname == null)
			hostname = "scratch.mit.edu";
		if (path == null)
			path = "/";
		URL url = new URL("https://" + hostname + path);
		HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

		try {
			conn.setDoOutput(true);
			conn.setDoInput(true);

			/*
			 * Sets up request statement and headers
			 */
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
			if (body != null) {
				allHeaders.put("Content-Length", Integer.toString(body.length));
				allHeaders.put("Content-Type", "application/json");
			}
			if (sessionId != null)
				allHeaders.put("Cookie", allHeaders.get("Cookie") + "scratchsessionsid=" + sessionId + ";");

			for (Entry<String, String> header : allHeaders.entrySet())
				conn.setRequestProperty(header.getKey(), header.getValue());

			/*
			 * Sends request body through the output stream of "conn"
			 */
			DataOutputStream connOut = new DataOutputStream(conn.getOutputStream());
			try {
				connOut.write(body);
				connOut.flush();
			} finally {
				connOut.close();
			}

			/*
			 * Reads response body from the input stream of "conn" into
			 * "responseBodyBuffer"
			 */
			BufferedReader connIn = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			try {
				StringBuffer responseBodyBuffer = new StringBuffer();
				String nextLine;
				while ((nextLine = connIn.readLine()) != null)
					responseBodyBuffer.append(nextLine);

				return new HttpResponse(conn.getHeaderFields(), responseBodyBuffer.toString());
			} finally {
				connIn.close();
			}
		} finally {
			conn.disconnect();
		}
	}
}
