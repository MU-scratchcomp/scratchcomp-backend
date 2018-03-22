package scratchreferee.scratchapi;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.net.URLEncoder;

import org.json.JSONObject;

import scratchreferee.scratchapi.ScratchNetworkUtil.HttpResponse;

public final class ScratchSession {

	private final String username;
	private final long id;
	private final String sessionId;

	public ScratchSession(String username, String password) throws IOException {
		Map<String, String> loginHeaders = new HashMap<String, String>();
		loginHeaders.put("X-Requested-With", "XMLHttpRequest");
		String loginCredentials = new JSONObject().put("username", username).put("password", password).toString();

		HttpResponse loginResponse = ScratchNetworkUtil.execute("POST", null, "/login/", loginHeaders, null,
				loginCredentials);

		sessionId = parseCookie(loginResponse.headerFields.get("Set-Cookie"), "securescratchsessionsid");

		JSONObject sessionDetails = loginResponse.parseBody();
		this.username = sessionDetails.getString("username");
		this.id = sessionDetails.getLong("id");
	}

	private static final String parseCookie(List<String> cookieLists, String searchCookie) {
		StringBuilder cookieListBuilder = new StringBuilder();
		for (String cookieList : cookieLists)
			cookieListBuilder.append(cookieList);
		String cookieList = cookieListBuilder.toString();

		String[] each = cookieList.split(";");
		for (String binding : each) {
			binding = binding.trim();
			String key = binding.substring(0, binding.indexOf('='));
			if (!key.equals(searchCookie))
				continue;
			String value = binding.substring(binding.indexOf('=') + 1);
			return value;
		}
		return null;
	}

	public long newProject(String name, String payload, Map<String, byte[]> media, byte[] thumbnail)
			throws IOException {
		/*
		 * Post media
		 */
		for (Entry<String, byte[]> item : media.entrySet())
			ScratchNetworkUtil.execute("POST", "assets.scratch.mit.edu",
					"/internalapi/asset/" + item.getKey() + "/set/" + newProjectQueryString(), null, sessionId,
					item.getValue());

		/*
		 * Post project payload
		 */
		HttpResponse response = ScratchNetworkUtil.execute("POST", "projects.scratch.mit.edu",
				"/internalapi/project/new/set/" + newProjectQueryString(new String[] { "title", name }), null,
				sessionId, payload);
		long projectId = response.parseBody().getLong("content-name");

		/*
		 * Post project thumbnail
		 */
		if (thumbnail != null && thumbnail.length > 0)
			ScratchNetworkUtil.execute("POST", "scratch.mit.edu",
					"/internalapi/project/thumbnail/" + projectId + "/set/" + newProjectQueryString(), null, sessionId,
					thumbnail);

		return projectId;
	}

	private String newProjectQueryString(String[]... parameters) throws UnsupportedEncodingException {
		String[][] allParameters = new String[parameters.length + 2][];
		allParameters[0] = new String[] { "v", "v459.1" };
		allParameters[1] = new String[] { "_rnd", Double.toString(Math.random()) };
		System.arraycopy(parameters, 0, allParameters, 2, parameters.length);

		String[] parameterStrings = new String[allParameters.length];
		for (int i = 0; i < allParameters.length; i++) {
			parameterStrings[i] = URLEncoder.encode(allParameters[i][0], "UTF-8") + "="
					+ URLEncoder.encode(allParameters[i][1], "UTF-8");
		}

		return "?" + String.join("&", parameterStrings);
	}

	public long newProject(String name, String payload) throws IOException {
		return newProject(name, payload, new HashMap<String, byte[]>(), null);
	}

	private static final String readDefaultProjectData() throws IOException {
		BufferedReader dataReader = new BufferedReader(
				new InputStreamReader(new FileInputStream("defaultprojectdata.txt")));
		StringBuffer dataBuffer = new StringBuffer();
		String nextLine;
		while ((nextLine = dataReader.readLine()) != null)
			dataBuffer.append(nextLine);
		dataReader.close();
		return dataBuffer.toString();
	}

	public long newProject() throws IOException {
		return newProject("Untitled", readDefaultProjectData());
	}

	public long newProject(String name, ScratchProject project) throws IOException {
		return newProject(name, project.payload.toString(), project.media, null);
	}
	
	public long newProject(String name, ScratchProject project, byte[] thumbnailPNG) throws IOException {
		return newProject(name, project.payload.toString(), project.media, thumbnailPNG);
	}
}
