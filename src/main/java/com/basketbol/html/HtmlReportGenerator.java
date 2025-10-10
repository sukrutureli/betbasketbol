package com.basketbol.html;

import com.basketbol.model.MatchInfo;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class HtmlReportGenerator {

    public static void generateHtml(List<MatchInfo> matches, String fileName) {
        File dir = new File("public");
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, fileName);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
            writer.write("<title>Basketbol Tahminleri</title></head><body>");
            writer.write("<h1>🏀 Basketbol Tahminleri</h1>");
            writer.write("<table border='1'><tr><th>Ev Sahibi</th><th>Deplasman</th><th>Tahmin</th></tr>");

            for (MatchInfo m : matches) {
                writer.write("<tr>");
                writer.write("<td>" + m.getHomeTeam() + "</td>");
                writer.write("<td>" + m.getAwayTeam() + "</td>");
                writer.write("<td>" + m.getPrediction() + "</td>");
                writer.write("</tr>");
            }

            writer.write("</table></body></html>");
            System.out.println("💾 HTML dosyası oluşturuldu: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
