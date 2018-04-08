package scratchreferee.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import scratchreferee.scratchapi.ScratchProject;
import scratchreferee.scratchapi.ScratchSession;

public class Competition {

	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.out.println("Usage: <command> <appdata_directory_path> <frontend_directory_path>");
			System.exit(0);
		}
		
		final String webPath = args[1];
		final String uploadsPath = webPath + "uploads";
		final Pattern uploadPattern = Pattern.compile("team([0-9]+)prob([0-9]+)(project|design).+(\\..+)");
		final Pattern clarifyPattern = Pattern.compile("clarify.*\\.txt");
		final String appdataPath = args[0];
		final String savePath = appdataPath + "/data/submissions";
		final Pattern savePattern = Pattern.compile("prob([0-9]+)sub([0-9]+).+");

		final String username = "MUScratchJudging17";
		final String password = "Dr.Gorliss";
		final Object loginMonitor = new Object();

		class Flag {
			private boolean flag = false;
			
			public boolean check() {
				return flag;
			}

			public boolean set() {
				boolean original = flag;
				flag = true;
				return !original;
			}
		}
		final Flag stopFlag = new Flag();

		Thread watcherThread = new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					try {
					if (stopFlag.check())
						break;

					File uploadsDirectory = new File(uploadsPath);
					uploadsDirectory.mkdirs();
					for (final File upload : uploadsDirectory.listFiles()) {
						/*
						 * Parse file
						 */
						Matcher m = uploadPattern.matcher(upload.getName());
						if (!m.matches()) {
							m = clarifyPattern.matcher(upload.getName());
							if (!m.matches()) {
								System.out.println(upload.getName() + " is not a matching file name.");
								continue;
							}
							
							/*
							 * Clarification file
							 */
							/*
							 * Move file out of uploads folder
							 */
							final File targetFolder = new File(savePath + "/clarify");
							if (!targetFolder.exists()) {
								targetFolder.mkdirs();
							}
							final File targetFile = new File(
									targetFolder.getAbsolutePath() + "/" + upload.getName());
							upload.renameTo(targetFile);
							System.out.println("Transferred file: " + upload.getName());
							
							continue;
						}

						System.out.println("Processing " + upload.getName());

						final int team = Integer.parseInt(m.group(1));
						final int problem = Integer.parseInt(m.group(2));
						final String type = m.group(3);
						final String extension = m.group(4);

						/*
						 * Move file out of uploads folder
						 */
						final File teamFolder = new File(savePath + "/team" + team);
						File targetFolder = (type.equals("project")) ? teamFolder
								: new File(teamFolder.getAbsolutePath() + "/design");
						int sub;
						if (!targetFolder.exists()) {
							targetFolder.mkdirs();
							sub = 1;
						} else {
							sub = targetFolder.list(new FilenameFilter() {

								@Override
								public boolean accept(File dir, String name) {
									Matcher saveMatcher = savePattern.matcher(name);
									return saveMatcher.matches() && Integer.parseInt(saveMatcher.group(1)) == problem;
								}
							}).length + 1;
						}
						final int submission = sub;

						final File targetFile = new File(
								targetFolder.getAbsolutePath() + "/prob" + problem + "sub" + submission + extension);
						upload.renameTo(targetFile);
						System.out.println("Transferred file: T" + team + "P" + problem + "S" + submission);

						/*
						 * Add index.csv entry
						 */
						final File indexFile = new File(savePath + "/index.csv");

						if ((type.equals("project") && !new File(
								teamFolder.getAbsolutePath() + "/design/prob" + problem + "sub" + submission + ".txt")
										.exists())
								|| type.equals("design") && !new File(
										teamFolder.getAbsolutePath() + "/prob" + problem + "sub" + submission + ".sb2")
												.exists()) {
							try {
								if (!indexFile.exists())
									indexFile.getParentFile().mkdirs();

								PrintWriter indexWriter = new PrintWriter(new FileOutputStream(
										new File(savePath + "/index.csv"), true));
								indexWriter.println(team + "," + problem + "," + submission + ",");
								indexWriter.flush();
								indexWriter.close();
								System.out.println("Wrote entry to index file: " + indexFile.getAbsolutePath());
							} catch (FileNotFoundException e) {
								e.printStackTrace();
							}
						}

						/*
						 * Upload project file
						 */
						if (type.equals("project")) {
							new Thread(new Runnable() {

								@Override
								public void run() {
									synchronized (loginMonitor) {
										for (int attempt = 0; attempt < 5; attempt++) {
										try {
											ScratchSession session = new ScratchSession(username, password);
											ScratchProject project = new ScratchProject(targetFile.getAbsolutePath());
											long projectID = session.newProject(
													"Team " + team + " - Q" + problem + " - Submission " + submission,
													project);

											StringBuffer bufferedIndex = new StringBuffer();
											Scanner indexScanner = new Scanner(new FileInputStream(indexFile));
											String searchPrefix = team + "," + problem + "," + submission + ",";
											while (indexScanner.hasNextLine()) {
												String line = indexScanner.nextLine();
												if (line.startsWith(searchPrefix)) {
													bufferedIndex.append(line + "https://scratch.mit.edu/projects/"
															+ projectID + "/");
												} else
													bufferedIndex.append(line + '\n');
											}
											indexScanner.close();

											PrintWriter indexWriter = new PrintWriter(new FileOutputStream(indexFile));
											indexWriter.print(bufferedIndex.toString());
											indexWriter.flush();
											indexWriter.close();

											System.out.println("Uploaded file: T" + team + "P" + problem + "S" + submission);
											break;
										} catch (IOException e) {
											System.err.println("Upload IOException for " + upload.getName());
											e.printStackTrace();
											try {
											loginMonitor.wait(2000);
											} catch (InterruptedException ex) {
												ex.printStackTrace();
											}
										}
										}
									}
								}
							}).start();
						}
					}
					} catch (RuntimeException e) {
						e.printStackTrace();
					}
				}
				System.out.println("Watcher shutting down.");
			}
		});
		watcherThread.start();

		Scanner kb = new Scanner(System.in);
		kb.nextLine();
		stopFlag.set();
		watcherThread.interrupt();
	}
}

