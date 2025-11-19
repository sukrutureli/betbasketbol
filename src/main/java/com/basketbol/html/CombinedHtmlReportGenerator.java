package com.basketbol.html;

import com.basketbol.MatchHistoryManager;
import com.basketbol.model.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class CombinedHtmlReportGenerator {

	public static void generateCombinedHtml(List<LastPrediction> sublistPredictions, List<MatchInfo> matches,
			MatchHistoryManager historyManager, List<Match> matchStats, List<PredictionResult> results,
			List<PredictionData> sublistPredictionData, String fileName, String day, List<RealScores> realScores) {
		try {
			// 1️⃣ Alt HTML'leri üret
			HtmlReportGenerator.generateHtmlForSublist(sublistPredictions, sublistPredictionData, "TEMP_SUB.html");
			HtmlReportGenerator.generateHtml(matches, historyManager, matchStats, results, "TEMP_DETAIL.html",
					realScores);

			File tempSub = new File("public/TEMP_SUB.html");
			File tempDetail = new File("public/TEMP_DETAIL.html");

			String subContent = Files.exists(tempSub.toPath()) ? Files.readString(tempSub.toPath()) : "";
			String detailContent = Files.exists(tempDetail.toPath()) ? Files.readString(tempDetail.toPath()) : "";

			String subBody = extractBody(subContent);
			String detailBody = extractBody(detailContent);
			String subStyle = stripBody(extractStyle(subContent));
			String detStyle = stripBody(extractStyle(detailContent));

			// 2️⃣ Nihai sayfa
			File dir = new File("public/basketbol");
			if (!dir.exists())
				dir.mkdirs();
			File output = new File(dir, fileName);

			try (FileWriter fw = new FileWriter(output)) {
				fw.write("<!DOCTYPE html><html lang='tr'><head><meta charset='UTF-8'>");
				fw.write("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
				fw.write("<title>🏀 Basketbol Tahminleri + 💰 Hazır Kupon</title>");
				fw.write("<style>");

				// 🔹 Global reset + layout → her şey aynı genişlikte
				fw.write("*,*::before,*::after{box-sizing:border-box;}");
				fw.write("html,body{margin:0;padding:0;}");
				fw.write("body{font-family:Arial,sans-serif;background:#f7f8fa;color:#222;}");
				fw.write("main{max-width:1200px;margin:0 auto;padding:0 10px;}");
				fw.write("section{margin:24px 0;}");
				fw.write("table{width:100%;border-collapse:collapse;}");
				fw.write("th,td{border:1px solid #ddd;}");

				// 🔹 Alt dosyalardan gelen stiller
				fw.write(subStyle);
				fw.write(detStyle);

				fw.write("</style></head><body>");
				fw.write("<main>");
				fw.write("<h1 style='text-align:center;margin:16px 0;'>" + day + "</h1>");

				fw.write("<section id='sublist'>");
				fw.write(subBody);
				fw.write("</section>");

				fw.write("<hr style='border:none;border-top:3px solid #0077cc;margin:24px 0;'>");

				fw.write("<section id='detailed'>");
				fw.write(detailBody);
				fw.write("</section>");

				fw.write("</main></body></html>");
			}

			// 3️⃣ Geçici dosyalar
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

	// body{} kuralını sil (çakışma olmasın)
	private static String stripBody(String css) {
		return css.replaceAll("body\\s*\\{[^}]*\\}", "");
	}
}
