package com.basketbol.scraper;

import com.basketbol.model.MatchInfo;
import com.basketbol.model.MatchResult;
import com.basketbol.model.Odds;
import com.basketbol.model.TeamMatchHistory;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BasketballScraper {

    private WebDriver driver;
    private JavascriptExecutor js;
    private WebDriverWait wait;

    public BasketballScraper() {
        setupDriver();
    }

    private void setupDriver() {
        System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
        ChromeOptions options = new ChromeOptions();
        options.addArguments(
                "--headless=new",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--window-size=1920,1080",
                "--disable-blink-features=AutomationControlled",
                "user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
        );
        driver = new ChromeDriver(options);
        js = (JavascriptExecutor) driver;
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    // =============================================================
    // GÜNLÜK MAÇLAR
    // =============================================================
    public List<MatchInfo> fetchMatches() {
        List<MatchInfo> list = new ArrayList<>();
        try {
            String date = LocalDate.now(ZoneId.of("Europe/Istanbul"))
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            String url = "https://www.nesine.com/iddaa/basketbol?et=2&dt=" + date;

            System.out.println("🔗 URL açılıyor: " + url);
            driver.manage().deleteAllCookies();
            driver.get(url);
            PageWaitUtils.safeWaitForLoad(driver, 25);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-test-id^='r_']")));

            List<Map<String, String>> raw = scrollAndCollectMatchData();
            System.out.println("🏀 Toplam basketbol maçı: " + raw.size());

            int index = 0;
            for (Map<String, String> d : raw) {
                Odds o = new Odds(
                        toDouble(d.get("ms1")),
                        toDouble(d.get("ms2")),
                        toDouble(d.get("h1Value")),
                        toDouble(d.get("h1")),
                        toDouble(d.get("h2")),
                        toDouble(d.get("h2Value")),
                        toDouble(d.get("alt")),
                        toDouble(d.get("limit")),
                        toDouble(d.get("ust"))
                );
                list.add(new MatchInfo(d.get("name"), d.get("time"), d.get("url"), o, index++));
            }

        } catch (Exception e) {
            System.out.println("fetchMatches hata: " + e.getMessage());
        }
        return list;
    }

    // =============================================================
    // SCROLL VE MAÇLARI TOPLA
    // =============================================================
    private List<Map<String, String>> scrollAndCollectMatchData() throws InterruptedException {
        By eventSelector = By.cssSelector("div[data-test-id^='r_'][data-sport-id='2']");
        Set<String> seen = new HashSet<>();
        List<Map<String, String>> collected = new ArrayList<>();

        int stable = 0, prevCount = 0;
        int maxScroll = 100;
        int scrollAmount = 800;

        while (driver.findElements(eventSelector).isEmpty()) Thread.sleep(500);
        System.out.println("⏳ Basketbol maçları göründü - scroll başlıyor...");

        for (int i = 0; i < maxScroll; i++) {
            List<WebElement> matches = driver.findElements(eventSelector);

            for (WebElement el : matches) {
                try {
                    WebElement nameEl = el.findElement(By.cssSelector("[data-test-id='matchName']"));
                    String name = nameEl.getText().trim();
                    if (name.isEmpty() || seen.contains(name)) continue;
                    seen.add(name);

                    Map<String, String> map = new HashMap<>();
                    map.put("name", name);
                    map.put("url", nameEl.getAttribute("href"));

                    // Saat
                    try {
                        String time = el.findElement(By.cssSelector("span[data-testid^='time']")).getText().trim();
                        map.put("time", time);
                    } catch (Exception ex) {
                        map.put("time", "-");
                    }

                    // MBS
                    try {
                        String mbs = el.findElement(By.cssSelector("[data-test-id='event_mbs'] span")).getText().trim();
                        map.put("mbs", mbs);
                    } catch (Exception e) {
                        map.put("mbs", "-1");
                    }

                    // MS1 / MS2
                    map.put("ms1", getOdd(el, "odd_Maç Sonucu_1"));
                    map.put("ms2", getOdd(el, "odd_Maç Sonucu_2"));

                    // Handikap (H1/H2)
                    map.put("h1Value", getOdd(el, "odd_Handikaplı Maç Sonucu_H1"));
                    map.put("h1", getOdd(el, "odd_Handikaplı Maç Sonucu_1"));
                    map.put("h2", getOdd(el, "odd_Handikaplı Maç Sonucu_2"));
                    map.put("h2Value", getOdd(el, "odd_Handikaplı Maç Sonucu_H2"));

                    // Alt / Üst
                    map.put("alt", getOdd(el, "odd_Alt/Üst_Alt"));
                    map.put("limit", getOdd(el, "odd_Alt/Üst_Limit"));
                    map.put("ust", getOdd(el, "odd_Alt/Üst_Üst"));

                    collected.add(map);
                    System.out.println("✅ " + name + " (" + map.get("time") + ") eklendi.");

                } catch (Exception ignore) {}
            }

            if (seen.size() == prevCount) stable++; else stable = 0;
            if (stable >= 3) {
                System.out.println("✅ Scroll tamamlandı (sabitliğe ulaşıldı)");
                break;
            }
            prevCount = seen.size();

            js.executeScript("window.scrollBy(0, " + scrollAmount + ");");
            Thread.sleep(700);
        }

        System.out.println("🧩 TOPLAM BENZERSİZ MAÇ: " + seen.size());
        return collected;
    }

    private String getOdd(WebElement el, String testId) {
        try {
            return el.findElement(By.cssSelector("[data-testid='" + testId + "']")).getText().trim();
        } catch (Exception e) {
            return "-";
        }
    }

    private double toDouble(String s) {
        try {
            if (s == null || s.equals("-") || s.isEmpty()) return 0.0;
            return Double.parseDouble(s.replace(",", "."));
        } catch (Exception e) {
            return 0.0;
        }
    }

    // =============================================================
    // GEÇMİŞ MAÇLAR (REKABET + SON MAÇLAR)
    // =============================================================
    public TeamMatchHistory scrapeTeamHistory(String detailUrl, String name, Odds odds) {
        if (detailUrl == null || !detailUrl.startsWith("http")) return null;

        String[] teams = extractTeamsFromHeader(detailUrl);
        String home = teams[0];
        String away = teams[1];
        String title = teams[2];

        TeamMatchHistory th = new TeamMatchHistory(title, home, away, detailUrl, odds);
        try {
            List<MatchResult> rekabet = scrapeRekabetGecmisi(detailUrl + "/rekabet-gecmisi");
            rekabet.forEach(th::addRekabetGecmisiMatch);

            List<MatchResult> sonHome = scrapeSonMaclar(detailUrl + "/son-maclari", 1);
            sonHome.forEach(m -> th.addSonMacMatch(m, 1));

            List<MatchResult> sonAway = scrapeSonMaclar(detailUrl + "/son-maclari", 2);
            sonAway.forEach(m -> th.addSonMacMatch(m, 2));
        } catch (Exception e) {
            System.out.println("scrapeTeamHistory hata: " + e.getMessage());
        }
        return th;
    }

    private List<MatchResult> scrapeRekabetGecmisi(String url) {
        List<MatchResult> list = new ArrayList<>();
        try {
            driver.get(url);
            PageWaitUtils.safeWaitForLoad(driver, 15);
            Thread.sleep(800);

            List<WebElement> rows = driver.findElements(By.cssSelector("div[data-test-id='CompitionHistoryTableItem']"));
            for (WebElement r : rows) {
                try {
                    String league = safeText(r, "[data-test-id='CompitionTableItemLeague']");
                    String date = safeText(r, "[data-test-id='CompitionTableItemSeason']");
                    String home = safeText(r, "div[data-test-id='HomeTeam']");
                    String away = safeText(r, "div[data-test-id='AwayTeam']");
                    String score = extractScore(r);
                    int[] sc = parseScore(score);
                    list.add(new MatchResult(home, away, sc[0], sc[1], date, league, "rekabet-gecmisi"));
                } catch (Exception ignore) {}
            }
        } catch (Exception e) {
            System.out.println("⚠️ Rekabet geçmişi hatası: " + e.getMessage());
        }
        return list;
    }

    private List<MatchResult> scrapeSonMaclar(String url, int side) {
        List<MatchResult> list = new ArrayList<>();
        try {
            driver.get(url);
            PageWaitUtils.safeWaitForLoad(driver, 15);
            Thread.sleep(800);

            String sel = (side == 1)
                    ? "div[data-test-id^='LastMatchesTable'][data-test-id*='Home'] tbody tr"
                    : "div[data-test-id^='LastMatchesTable'][data-test-id*='Away'] tbody tr";

            List<WebElement> rows = driver.findElements(By.cssSelector(sel));
            for (WebElement r : rows) {
                try {
                    String league = safeText(r, "td[data-test-id='TableBodyLeague']");
                    String home = safeText(r, "div[data-test-id='HomeTeam']");
                    String away = safeText(r, "div[data-test-id='AwayTeam']");
                    String score = extractScore(r);
                    int[] sc = parseScore(score);
                    list.add(new MatchResult(home, away, sc[0], sc[1], league, "", "son-maclari"));
                } catch (Exception ignore) {}
            }
        } catch (Exception e) {
            System.out.println("⚠️ Son maç hatası: " + e.getMessage());
        }
        return list;
    }

    private String extractScore(WebElement r) {
        try {
            List<WebElement> scoreEls = r.findElements(By.cssSelector("div[data-test-id='Score'], button[data-test-id='NsnButton'] span"));
            for (WebElement s : scoreEls) {
                String t = s.getText().replaceAll("\\(.*?\\)", "").trim();
                if (t.matches("\\d+\\s*-\\s*\\d+"))
                    return t;
            }
        } catch (Exception ignored) {}
        return "-";
    }

    private int[] parseScore(String s) {
        try {
            String[] p = s.split("-");
            return new int[]{Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim())};
        } catch (Exception e) {
            return new int[]{-1, -1};
        }
    }

    private String safeText(WebElement parent, String css) {
        try {
            return parent.findElement(By.cssSelector(css)).getText().trim();
        } catch (Exception e) {
            return "-";
        }
    }

    private String[] extractTeamsFromHeader(String url) {
        String home = "-", away = "-", name = "";
        try {
            driver.get(url);
            PageWaitUtils.waitForPageLoad(driver, 12);
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[data-test-id='HeaderTeams']")));

            WebElement header = driver.findElement(By.cssSelector("div[data-test-id='HeaderTeams']"));
            List<WebElement> teams = header.findElements(By.cssSelector("a[data-test-id='TeamLink'] span[data-test-id='HeaderTeams']"));

            if (teams.size() >= 2) {
                home = teams.get(0).getText().trim();
                away = teams.get(1).getText().trim();
            }
        } catch (Exception e) {
            System.out.println("Takım adları çekilemedi: " + e.getMessage());
        }
        name = home + " - " + away;
        return new String[]{home, away, name};
    }

    public void close() {
        try { driver.quit(); } catch (Exception ignore) {}
    }
}
