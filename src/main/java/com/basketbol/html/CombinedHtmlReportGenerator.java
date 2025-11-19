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
			// 1️⃣ Geçici HTML üret
			HtmlReportGenerator.generateHtmlForSublist(sublistPredictions, sublistPredictionData, "TEMP_SUB.html");
			HtmlReportGenerator.generateHtml(matches, historyManager, matchStats, results, "TEMP_DETAIL.html",
					realScores);

			File tempSub = new File("public/TEMP_SUB.html");
			File tempDetail = new File("public/TEMP_DETAIL.html");

			String subContent = Files.exists(tempSub.toPath()) ? Files.readString(tempSub.toPath()) : "";
			String detailContent = Files.exists(tempDetail.toPath()) ? Files.readString(tempDetail.toPath()) : "";

			String subBody = extractBody(subContent);
			String detailBody = extractBody(detailContent);
			String subStyle = extractStyle(subContent);
			String detailStyle = extractStyle(detailContent);

			// 2️⃣ Alt HTML'lerin body kurallarını temizle (aksi halde width bozulur)
			subStyle = subStyle.replaceAll("body\\s*\\{[^}]*\\}", "");
			detailStyle = detailStyle.replaceAll("body\\s*\\{[^}]*\\}", "");

			// 3️⃣ CSS scope et (çakışmayı engelle)
			String scopedSubStyle = subStyle.replace("h1", "#sublist h1").replace("table", "#sublist table")
					.replace("th", "#sublist th").replace("td", "#sublist td")
					.replace(".match-mbs", "#sublist .match-mbs").replace(".won", "#sublist .won")
					.replace(".lost", "#sublist .lost").replace(".pending", "#sublist .pending");

			String scopedDetailStyle = detailStyle.replace("h1", "#detailed h1").replace(".match", "#detailed .match")
					.replace(".odds", "#detailed .odds").replace(".quick-summary", "#detailed .quick-summary")
					.replace("table", "#detailed table").replace("th", "#detailed th").replace("td", "#detailed td");

			// 4️⃣ Nihai birleşik dosya üret
			File dir = new File("public/basketbol");
			if (!dir.exists())
				dir.mkdirs();
			File output = new File(dir, fileName);

			try (FileWriter fw = new FileWriter(output)) {

				fw.write("<!DOCTYPE html><html lang='tr'><head><meta charset='UTF-8'>");
				fw.write("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
				fw.write("<title>🏀 Basketbol Tahminleri + 💰 Hazır Kupon</title>");

				fw.write("<style>");
				fw.write("html,body{margin:0;padding:0;}");

				fw.write("body{font-family:Arial,sans-serif;background:#f7f8fa;color:#222;}");

				/* GENEL GENİŞLİK KONTROLÜ */
				fw.write("section{width:100%;max-width:1200px;margin:30px auto;padding:0 12px;box-sizing:border-box;}");

				/* AYIRICI */
				fw.write("hr{border:none;border-top:3px solid #0077cc;margin:40px 0;}");

				/* --- SUBLIST TABLO --- */
				fw.write("#sublist table{width:100%;border-collapse:collapse;background:#fff;"
						+ "border-radius:8px;box-shadow:0 2px 8px rgba(0,0,0,0.1);}");

				fw.write("#sublist th,#sublist td{padding:10px 12px;text-align:center;"
						+ "border:1px solid #ddd;box-sizing:border-box;}");

				fw.write("#sublist th{background:#0077cc;color:#fff;font-size:15px;}");
				fw.write("#sublist tr:nth-child(even){background:#f3f6fa;}");
				fw.write("#sublist tr:hover{background:#eaf3ff;}");

				fw.write(".status-icon{font-size:1.2em}");
				fw.write(".won{color:#28a745}.lost{color:#dc3545}.pending{color:#999}");

				/* --- DETAILED MATCH CONTAINER --- */
				fw.write(".match{background:#fff;border:1px solid #dce3ec;margin:18px 0;padding:18px;"
						+ "border-radius:12px;box-shadow:0 2px 8px rgba(0,0,0,0.08);}");

				fw.write(".match-header{display:flex;justify-content:space-between;align-items:center;"
						+ "flex-wrap:wrap;margin-bottom:10px;}");

				fw.write(".match-name{font-weight:700;color:#003366;font-size:1.5em;}");
				fw.write(".match-time{color:#004d80;font-size:1.3em;font-weight:600;}");

				/* --- ODDS GRID --- */
				fw.write(".odds-mini{background:#f8fafc;border:1px solid #dbe2ea;padding:12px;"
						+ "border-radius:10px;margin:14px 0;}");

				fw.write(".odds-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(120px,1fr));gap:10px;}");

				fw.write(".odds-cell{background:#fff;border:1px solid #ccd6e0;border-radius:8px;"
						+ "padding:8px;text-align:center;box-shadow:0 1px 3px rgba(0,0,0,0.06);}");

				/* --- QUICK SUMMARY TABLOSU --- */
				fw.write(".qs{width:100%;border-collapse:collapse;background:#fff;border:1px solid #ccd6e0;"
						+ "border-radius:8px;overflow:hidden;margin-top:10px;}");

				fw.write(".qs th,.qs td{padding:8px;text-align:center;border:1px solid #e1e7ef;}");

				fw.write(".qs th{background:#f0f5fb;color:#003366;font-weight:600;}");

				/* --- TEAM STATS --- */
				fw.write(".team-stats{background:#e3f2fd;color:#0c5460;padding:10px;border-radius:6px;margin:8px 0;}");

				/* --- RESPONSIVE --- */
				fw.write("@media(max-width:600px){"
						+ ".match-header{flex-direction:column;align-items:flex-start;gap:6px;}"
						+ ".match-time{font-size:1.1em;}" + ".odds-grid{grid-template-columns:repeat(2,1fr);}"
						+ "section{padding:0 6px;}" + "table{font-size:14px;}" + "}");

				fw.write("</style>");

				fw.write("</head><body>");

				fw.write("<h1 style='text-align:center;margin:16px 0;'>" + day + "</h1>");

				fw.write("<section id='sublist'>");
				fw.write(subBody);
				fw.write("</section>");

				fw.write("<hr>");

				fw.write("<section id='detailed'>");
				fw.write(detailBody);
				fw.write("</section>");

				fw.write("</body></html>");
			}

			// 5️⃣ Geçici dosyaları sil
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
