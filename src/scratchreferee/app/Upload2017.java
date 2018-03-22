package scratchreferee.app;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.json.JSONObject;

import scratchreferee.scratchapi.ScratchProject;
import scratchreferee.scratchapi.ScratchSession;

public class Upload2017 {

	public static void main(String[] args) throws IOException {
		File submissionsDir = new File(String.join(File.separator, "..", "ScratchCompResults", "submissions"));

		Pattern teamPattern = Pattern.compile("team([0-9]+)");
		Pattern sb2Pattern = Pattern.compile("prob([0-9]+)sub([0-9]+)\\.sb2");

		File[] teamFolders = submissionsDir.listFiles((FileFilter) new WildcardFileFilter("team?*"));
		Team[] teams = new Team[teamFolders.length];

		for (int iTeam = 0; iTeam < teamFolders.length; iTeam++) {
			Team team = teams[iTeam] = new Team();
			team.folder = teamFolders[iTeam];
			System.out.println(team.folder.getPath());

			Matcher teamMatch = teamPattern.matcher(team.folder.getName());
			teamMatch.find();
			team.number = Integer.parseInt(teamMatch.group(1));

			File[] sb2Files = team.folder.listFiles((FileFilter) new WildcardFileFilter("prob*sub*.sb2"));
			team.submissions = new Submission[sb2Files.length];

			for (int iSubmission = 0; iSubmission < sb2Files.length; iSubmission++) {
				Submission submission = team.submissions[iSubmission] = new Submission();
				submission.file = sb2Files[iSubmission];
				System.out.println(submission.file.getPath());

				Matcher sb2Match = sb2Pattern.matcher(submission.file.getName());
				sb2Match.find();
				submission.problem = Integer.parseInt(sb2Match.group(1));
				submission.number = Integer.parseInt(sb2Match.group(2));
			}
			Arrays.sort(team.submissions,
					(s1, s2) -> (s1.problem != s2.problem) ? s1.problem - s2.problem : s1.number - s2.number);
		}
		Arrays.sort(teams, (t1, t2) -> t1.number - t2.number);

		ScratchSession session = new ScratchSession("MUScratchJudging17", "Dr.Gorliss");

		BufferedWriter uploadCsvWriter = Files.newBufferedWriter(submissionsDir.toPath().resolve("uploadIndex.csv"));
		CSVPrinter uploadCsvPrinter = new CSVPrinter(uploadCsvWriter, CSVFormat.DEFAULT.withHeader("Team", "Problem",
				"Submission", "Scratch Project ID", "Score", "Judge", "Feedback", "Design Document"));

		System.out.println();

		for (Team team : teams) {
			for (Submission submission : team.submissions) {
				String projectName = String.join(" - ", "Team " + team.number, "Problem " + submission.problem,
						"Submission " + submission.number);
				String thumbnailText = String.join(" ", "T" + team.number, "P" + submission.problem,
						"S" + submission.number);

				long scratchProjectID = session.newProject(projectName, new ScratchProject(submission.file.getPath()),
						textToThumbnail(thumbnailText, team.number, teams.length));
				System.out.println(projectName + " - " + scratchProjectID);

				File feedbackFile = team.folder.toPath().resolve("feedback")
						.resolve("prob" + submission.problem + "sub" + submission.number + ".txt").toFile();
				String score, judge, feedback;
				if (feedbackFile.exists()) {
					JSONObject feedbackData = new JSONObject(
							IOUtils.toString(feedbackFile.toURI(), Charset.defaultCharset()));
					score = jsonRead(feedbackData, "score");
					judge = jsonRead(feedbackData, "judge");
					feedback = jsonRead(feedbackData, "feedback");
				} else {
					score = judge = feedback = "";
				}

				File designFile = team.folder.toPath().resolve("design")
						.resolve("prob" + submission.problem + "sub" + submission.number + ".txt").toFile();
				String design;
				if (designFile.exists()) {
					design = IOUtils.toString(designFile.toURI(), Charset.defaultCharset());
				} else {
					design = "";
				}

				uploadCsvPrinter.printRecord(team.number, submission.problem, submission.number, scratchProjectID,
						score, judge, feedback, design);
			}
			uploadCsvPrinter.flush();
		}

		uploadCsvPrinter.close();
	}

	private static class Team {
		File folder;
		int number;
		Submission[] submissions;
	}

	private static class Submission {
		File file;
		int problem;
		int number;
	}

	private static String jsonRead(JSONObject obj, String key) {
		try {
			return obj.getString(key);
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	private static byte[] textToThumbnail(String text, int team, int teams) throws IOException {
		BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = img.createGraphics();
		Font font = new Font("Arial", Font.BOLD, 90);
		g2d.setFont(font);
		FontMetrics fm = g2d.getFontMetrics();
		int stringWidth = fm.stringWidth(text);
		int stringHeight = fm.getHeight();
		g2d.dispose();

		int imageWidth = 480, imageHeight = 360;
		img = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
		g2d = img.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
		g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2d.setFont(font);
		fm = g2d.getFontMetrics();
		g2d.setColor(Color.getHSBColor(((float) team / (float) Math.PI) % 1.0f, 1.0f, 0.5f));
		g2d.drawString(text, imageWidth / 2 - stringWidth / 2, fm.getAscent() + imageHeight / 2 - stringHeight / 2);
		g2d.dispose();

		ByteArrayOutputStream imageRender = new ByteArrayOutputStream();
		ImageIO.write(img, "png", imageRender);
		return imageRender.toByteArray();
	}
}
