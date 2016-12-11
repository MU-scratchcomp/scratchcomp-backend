package scratchreferee.app;

import java.io.IOException;

import scratchreferee.scratchapi.ScratchProject;
import scratchreferee.scratchapi.ScratchSession;

public class PostProjectTest {

	public static void main(String[] args) throws IOException {
		main_test(args);
	}
	
	private static void main_test(String[] args) throws IOException {
		ScratchSession session = new ScratchSession("mojo2016", "Dr.Gorliss");
		
		ScratchProject project = new ScratchProject("exampleprojectdata.sb2");
		System.out.println("Uploaded project ID: " + session.newProject("exampleprojectdata", project));
	}

	@SuppressWarnings("unused")
	private static void main_diagnostic(String[] args) throws IOException {
		long startTime = System.currentTimeMillis();
		
		ScratchSession session = new ScratchSession("mojo2016", "Dr.Gorliss");
		long nextTime = System.currentTimeMillis();
		System.out.println("Logged in: took " + (nextTime - startTime) + "ms");
		startTime = nextTime;
		
		ScratchProject project = new ScratchProject("exampleprojectdata.sb2");
		nextTime = System.currentTimeMillis();
		System.out.println("Read project file: took " + (nextTime - startTime) + "ms");
		startTime = nextTime;
		
		for (String fileName : project.media.keySet())
			System.out.println(fileName);
		
		long projectid = session.newProject("exampleprojectdata", project);
		nextTime = System.currentTimeMillis();
		System.out.println("Uploaded project: took " + (nextTime - startTime) + "ms");
		startTime = nextTime;
		
		System.out.println("Uploaded project ID: " + projectid);
	}
}
