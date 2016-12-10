package scratchreferee.app;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import scratchreferee.scratchapi.ScratchNetworkUtil;
import scratchreferee.scratchapi.ScratchNetworkUtil.HttpResponse;

public class Test {
	
	public static void main(String[] args) throws JSONException, IOException {
		final String username = "mojo2016";
		final String password = "Dr.Gorliss";
		
		Map<String, String> loginHeaders = new HashMap<String, String>();
		loginHeaders.put("X-Requested-With", "XMLHttpRequest");
		
		HttpResponse response = ScratchNetworkUtil.execute("POST", null, "/login/", loginHeaders, null, new JSONObject().put("username", username).put("password", password).toString());
		System.out.println(response.headerFieldsToString());
		System.out.println(response.body);
	}

}
