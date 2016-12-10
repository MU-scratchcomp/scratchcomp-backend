package scratchreferee.app;

import java.io.IOException;

import scratchreferee.scratchapi.ScratchSession;

public class NewDefaultProjectTest {

	public static void main(String[] args) throws IOException {
		ScratchSession session = new ScratchSession("mojo2016", "Dr.Gorliss");
		System.out.println("New project ID: " + session.newProject());
	}

}
