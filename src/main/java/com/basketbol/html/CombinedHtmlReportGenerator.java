package com.basketbol.html;

import com.basketbol.MatchHistoryManager;
import com.basketbol.model.*;

import java.io.*;
import java.nio.file.Files;
import java.util.List;

public class CombinedHtmlReportGenerator {

	public static void generateCombinedHtml(List<LastPrediction> sublistPredictions, List<MatchInfo> matches,
			MatchHistoryManager historyManager, List<Match> matchStats, List<PredictionResult> results,
			List<PredictionData> sublistPredictionData, String fileName, String day) {
		try {
			// 1️⃣ Geçici dosyaları oluştur
			HtmlReportGenerator.generateHtmlForSublist(sublistPredictions, sublistPredictionData, "TEMP_SUB.html");
			HtmlReportGenerator.generateHtml(matches, historyManager, matchStats, results, "TEMP_DETAIL.html");

			File tempSub = new File("public/TEMP_SUB.html");
			File tempDetail = new File("public/TEMP_DETAIL.html");

			String subContent = Files.exists(tempSub.toPath()) ? Files.readString(tempSub.toPath()) : "";
			String detailContent = Files.exists(tempDetail.toPath()) ? Files.readString(tempDetail.toPath()) : "";

			String subBody = extractBody(subContent);
			String detailBody = extractBody(detailContent);
			String subStyle = extractStyle(subContent);
			String detailStyle = extractStyle(detailContent);

			// 2️⃣ Nihai birleşik dosya
			File dir = new File("public/basketbol");
			if (!dir.exists())
				dir.mkdirs();
			File output = new File(dir, fileName);

			try (FileWriter fw = new FileWriter(output)) {
				fw.write("<!DOCTYPE html><html lang='tr'><head><meta charset='UTF-8'>");
				fw.write("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
				fw.write("<title>🏀 Basketbol Tahminleri + 💰 Hazır Kupon</title>");
				fw.write("<style>");

				fw.write("#sublist {\n");
				fw.write("   width: 100%;\n");
				fw.write("   box-sizing: border-box;\n");
				fw.write("}\n");
				fw.write("#sublist * { box-sizing: border-box; }\n");

				fw.write(subStyle.replace("body {", "#sublist {").replace("body,", "#sublist,"));

				fw.write("#detailed {\n");
				fw.write("   width: 100%;\n");
				fw.write("   box-sizing: border-box;\n");
				fw.write("}\n");
				fw.write("#detailed * { box-sizing: border-box; }\n");

				fw.write(detailStyle.replace("body {", "#detailed {").replace("body,", "#detailed,"));

				fw.write(
						"section{margin:30px auto; max-width:1200px;} hr{border:none;border-top:3px solid #0077cc;margin:40px 0;}");

				fw.write("</style>");

				fw.write("</head><body>");
				fw.write("<h1>" + day + "</h1>");
				fw.write("<section id='sublist'>");
				fw.write(subBody);
				fw.write("</section><hr>");
				fw.write("<section id='detailed'>");
				fw.write(detailBody);
				fw.write("</section></body></html>");
			}

			tempSub.delete();
			tempDetail.delete();
			System.out.println("✅ Birleşik basketbol HTML üretildi: " + output.getAbsolutePath());

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String extractBody(String html) {
		int s = html.indexOf("<body>");
		int e = html.indexOf("</body>");
		return (s != -1 && e != -1) ? html.substring(s + 6, e) : html;
	}

	private static String extractStyle(String html) {
		int s = html.indexOf("<style>");
		int e = html.indexOf("</style>");
		return (s != -1 && e != -1) ? html.substring(s + 7, e) : "";
	}
}
