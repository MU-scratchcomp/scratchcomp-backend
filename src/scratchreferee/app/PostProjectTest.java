package scratchreferee.app;

import java.io.IOException;

import scratchreferee.scratchapi.ScratchSession;

public class PostProjectTest {

	public static void main(String[] args) throws IOException {
		ScratchSession session = new ScratchSession("mojo2016", "Dr.Gorliss");
		long projectid = session.newProject("exampleprojectdata.sb2");
		System.out.println("Uploaded project ID: " + projectid);
	}

}
