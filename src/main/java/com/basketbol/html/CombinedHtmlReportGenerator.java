package com.basketbol.html;

import com.basketbol.MatchHistoryManager;
import com.basketbol.model.*;

import java.io.*;
import java.nio.file.Files;
import java.util.List;

public class CombinedHtmlReportGenerator {

	public static void generateCombinedHtml(List<LastPrediction> sublistPredictions, List<MatchInfo> matches,
			MatchHistoryManager historyManager, List<Match> matchStats, List<PredictionResult> results,
			List<PredictionData> sublistPredictionData, String fileName, String day, List<RealScores> realScores) {
		try {
			// 1️⃣ Geçici dosyaları oluştur
			HtmlReportGenerator.generateHtmlForSublist(sublistPredictions, sublistPredictionData, "TEMP_SUB.html");
			HtmlReportGenerator.generateHtml(matches, historyManager, matchStats, results, "TEMP_DETAIL.html", realScores);

			File tempSub = new File("public/TEMP_SUB.html");
			File tempDetail = new File("public/TEMP_DETAIL.html");

			String subContent = Files.exists(tempSub.toPath()) ? Files.readString(tempSub.toPath()) : "";
			String detailContent = Files.exists(tempDetail.toPath()) ? Files.readString(tempDetail.toPath()) : "";

			String subBody = extractBody(subContent);
			String detailBody = extractBody(detailContent);
			String subStyle = extractStyle(subContent);
			String detailStyle = extractStyle(detailContent);

			// 2️⃣ Eski body kurallarını temizle (yoksa padding/margin karışıyor)
			subStyle = subStyle.replaceAll("body\\s*\\{[^}]*\\}", "");
			detailStyle = detailStyle.replaceAll("body\\s*\\{[^}]*\\}", "");

			// 3️⃣ CSS’i #sublist ve #detailed altında scope et
			String scopedSubStyle = subStyle.replace("h1", "#sublist h1").replace("table", "#sublist table")
					.replace("th", "#sublist th").replace("td", "#sublist td")
					.replace(".match-mbs", "#sublist .match-mbs").replace(".won", "#sublist .won")
					.replace(".lost", "#sublist .lost").replace(".pending", "#sublist .pending");

			String scopedDetailStyle = detailStyle.replace("h1", "#detailed h1").replace(".match", "#detailed .match")
					.replace(".odds", "#detailed .odds").replace(".quick-summary", "#detailed .quick-summary")
					.replace("table", "#detailed table").replace("th", "#detailed th").replace("td", "#detailed td");

			// 4️⃣ Nihai birleşik dosya
			File dir = new File("public/basketbol");
			if (!dir.exists())
				dir.mkdirs();
			File output = new File(dir, fileName);

			try (FileWriter fw = new FileWriter(output)) {
				fw.write("<!DOCTYPE html><html lang='tr'><head><meta charset='UTF-8'>");
				fw.write("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
				fw.write("<title>🏀 Basketbol Tahminleri + 💰 Hazır Kupon</title>");
				fw.write("<style>");

				// 🔹 Global body + layout → MOBİLDE TAM GENİŞLİK
				fw.write("html,body{margin:0;padding:0;}");
				fw.write("body{font-family:Arial,sans-serif;background:#f7f8fa;color:#222;}");
				fw.write(
						"#sublist,#detailed{width:100%;max-width:100%;margin:0 auto;padding:0 10px;box-sizing:border-box;}");
				fw.write("#sublist table,#detailed table{width:100%;border-collapse:collapse;}");
				fw.write("section{margin:30px auto;} ");
				fw.write("hr{border:none;border-top:3px solid #0077cc;margin:40px 0;}");

				// 🔹 Scope edilmiş stiller
				fw.write(scopedSubStyle);
				fw.write(scopedDetailStyle);

				fw.write("</style>");
				fw.write("</head><body>");

				fw.write("<h1 style='text-align:center;margin:16px 0;'>" + day + "</h1>");

				fw.write("<section id='sublist'>");
				fw.write(subBody);
				fw.write("</section><hr>");

				fw.write("<section id='detailed'>");
				fw.write(detailBody);
				fw.write("</section>");

				fw.write("</body></html>");
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
