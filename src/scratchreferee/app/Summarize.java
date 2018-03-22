package scratchreferee.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

public class Summarize {
			public static void main(String[] args) throws FileNotFoundException {
							Pattern submissionPattern = Pattern.compile("prob([0-9]+)sub([0-9]+).txt");

									String submissionsPath = "/users/majors/cmorley/scratchcomp3/submissions";
											
											double[][] scores = new double[24][6];
													for (int team = 1; team <= 23; team++) {
																		try {
																								File teamFolder = new File(submissionsPath + "/team" + team + "/feedback");

																												for (File file : teamFolder.listFiles()) {
																																			if (file.isDirectory())
																																											continue;
																																								Matcher m = submissionPattern.matcher(file.getName());
																																													if (!m.matches()) {
																																																					System.out.println("File " + file.getPath() + " does not match.");
																																																											continue;
																																																																}

																																																		int problem = Integer.parseInt(m.group(1));

																																																							String targetFileStr = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
																																																												
																																																												JSONObject j = new JSONObject(targetFileStr);
																																																																	String scoreString = j.getString("score");
																																																																						double score = Double.parseDouble(scoreString);
																																																																											System.out.println("Feedback file " + file.getPath() + " has score " + scoreString);

																																																																																
																																																																																if (score > scores[team][problem])
																																																																																								scores[team][problem] = score;

																																																																																				}
																															} catch (Exception e) {
																																					e.printStackTrace();
																																								}	
																				}
															
															File outputFile = new File("/users/majors/cmorley/scores.csv");
																	PrintWriter out = new PrintWriter(new FileOutputStream(outputFile));
																			
																			for (int team = 1; team <= 23; team++) {
																								String line = team + ",";
																											for (int prob = 1; prob <= 5; prob++) {
																																	line += scores[team][prob] + ",";
																																				}
																														line = line.substring(0, line.length() - 1);
																																	out.println(line);
																																			}
																					
																					out.close();
																							System.out.println("File written.");
																								}
}

