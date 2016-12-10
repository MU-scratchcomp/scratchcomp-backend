package scratchreferee.scratchapi;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

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

	public long newProject(String name, String payload) throws IOException {
		HttpResponse response = ScratchNetworkUtil.execute("POST", "projects.scratch.mit.edu",
				"/internalapi/project/new/set/?v=v452.1&_rnd=0.5618316377513111&title=" + name, null, sessionId,
				payload);
		return response.parseBody().getLong("content-name");
	}
	
	private static final String readStream(InputStream stream) throws IOException {
		BufferedReader dataReader = new BufferedReader(
				new InputStreamReader(stream));
		StringBuffer dataBuffer = new StringBuffer();
		String nextLine;
		while ((nextLine = dataReader.readLine()) != null)
			dataBuffer.append(nextLine);
		dataReader.close();
		return dataBuffer.toString();
	}
	
	private static final String readFile(String fileName) throws IOException {
		return readStream(new FileInputStream(fileName));
	}
	
	private static final String readDefaultProjectData() throws IOException {
		return readFile("defaultprojectdata.txt");
	}
	
	public long newProject() throws IOException {
		return newProject("Untitled", readDefaultProjectData());
	}
	
	public long newProject(String sb2FileName) throws IOException {
		String projectName = sb2FileName.substring(0, sb2FileName.length() - 4);
		ZipFile project = new ZipFile(sb2FileName);
		String payload = readStream(project.getInputStream(project.getEntry("project.json")));
		// TODO upload project media, i.e. sounds and graphics
		project.close();
		return newProject(projectName, payload);
	}
}
