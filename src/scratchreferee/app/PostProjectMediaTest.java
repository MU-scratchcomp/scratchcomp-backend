package scratchreferee.app;

import java.io.IOException;

import scratchreferee.scratchapi.ScratchProject;
import scratchreferee.scratchapi.ScratchSession;

public class PostProjectMediaTest {

	public static void main(String[] args) throws IOException {
		ScratchSession session = new ScratchSession("mojo2016", "Dr.Gorliss");
		
		ScratchProject project = new ScratchProject("samplefullproject.sb2");
		System.out.println("Uploaded project ID: " + session.newProject("samplefullproject", project));
	}

}
