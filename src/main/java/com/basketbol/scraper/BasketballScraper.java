package com.basketbol.scraper;

import com.basketbol.model.MatchInfo;
import com.basketbol.model.MatchResult;
import com.basketbol.model.Odds;
import com.basketbol.model.TeamMatchHistory;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BasketballScraper {
    private WebDriver driver;
    private JavascriptExecutor js;
    private WebDriverWait wait;

    public BasketballScraper() {
        setupDriver();
    }

    private void setupDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu",
                "--window-size=1920,1080", "--disable-extensions", "--disable-blink-features=AutomationControlled",
                "--disable-default-apps", "--disable-background-timer-throttling",
                "--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        options.setBinary("/usr/bin/google-chrome");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);

        this.driver = new ChromeDriver(options);
        this.js = (JavascriptExecutor) driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    public List<MatchInfo> fetchMatches() {
        List<MatchInfo> list = new ArrayList<>();
        ZoneId turkeyZone = ZoneId.of("Europe/Istanbul");
        LocalDate today = LocalDate.now(turkeyZone);
        String todayStr = today.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        try {
            String url = "https://www.nesine.com/iddaa/basketbol?et=2&dt=" + todayStr + "&le=2&ocg=MS&gt=Popüler";
            driver.get(url);
            PageWaitUtils.safeWaitForLoad(driver, 10);
            performScrolling();

            List<WebElement> events = driver.findElements(By.cssSelector("div.odd-col.event-list.pre-event"));
            System.out.println("Final element sayısı: " + events.size());

            for (int idx = 0; idx < events.size(); idx++) {
                try {
                    WebElement event = events.get(idx);
                    MatchInfo matchInfo = extractMatchInfo(event, idx);
                    if (matchInfo != null) list.add(matchInfo);
                } catch (Exception e) {
                    System.out.println("Element " + idx + " işlenirken hata: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.out.println("Ana sayfa scraping hatası: " + e.getMessage());
        }
        return list;
    }

    private void performScrolling() {
        try {
            int previousCount = -1, stableRounds = 0;
            while (stableRounds < 3) {
                List<WebElement> matches = driver.findElements(By.cssSelector("div.odd-col.event-list.pre-event"));
                int currentCount = matches.size();
                js.executeScript("window.scrollBy(0, 1500);");
                Thread.sleep(1000);
                stableRounds = (currentCount == previousCount) ? stableRounds + 1 : 0;
                previousCount = currentCount;
            }
        } catch (Exception e) {
            System.out.println("Scroll işlemi hatası: " + e.getMessage());
        }
    }

    private MatchInfo extractMatchInfo(WebElement event, int idx) {
        try {
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", event);
            Thread.sleep(200);

            String matchName = "İsim bulunamadı";
            String detailUrl = null;

            List<WebElement> nameLinks = event.findElements(By.cssSelector("div.name a"));
            if (!nameLinks.isEmpty()) {
                WebElement link = nameLinks.get(0);
                matchName = link.getText().trim();
                detailUrl = link.getAttribute("href");
            }

            if (detailUrl == null || detailUrl.isEmpty()) {
                for (WebElement link : event.findElements(By.tagName("a"))) {
                    String href = link.getAttribute("href");
                    if (href != null && href.contains("istatistik.nesine.com")) {
                        detailUrl = href;
                        if (matchName.equals("İsim bulunamadı")) {
                            String t = link.getText().trim();
                            if (!t.isEmpty()) matchName = t;
                        }
                        break;
                    }
                }
            }

            String matchTime = extractMatchTime(event);
            Odds odds = extractOdds(event);

            if (matchName.equals("İsim bulunamadı") && matchTime.equals("Zaman bulunamadı")) {
                return null;
            }

            return new MatchInfo(matchName, matchTime, detailUrl, odds, idx);
        } catch (Exception e) {
            System.out.println("Element " + idx + " extract hatası: " + e.getMessage());
            return null;
        }
    }

    private String extractMatchTime(WebElement event) {
        try {
            List<WebElement> timeList = event.findElements(By.cssSelector("div.time > span"));
            return !timeList.isEmpty() ? timeList.get(0).getText().trim() : "Zaman bulunamadı";
        } catch (Exception e) {
            return "Zaman hatası";
        }
    }

    private Odds extractOdds(WebElement event) {
        String[] odds = {"-", "-", "-", "-", "-", "-", "-", "-", "-"};
        try {
            List<WebElement> mainOdds = event.findElements(By.cssSelector("dd.col-02.event-row .cell"));
            for (int i = 0; i < Math.min(2, mainOdds.size()); i++) {
                String text = mainOdds.get(i).getText().trim();
                odds[i] = text.isEmpty() ? "-" : text;
            }
        } catch (Exception ignored) {}
        return new Odds(toDouble(odds[0]), toDouble(odds[1]), toDouble(odds[2]), toDouble(odds[3]), toDouble(odds[4]),
                toDouble(odds[5]), toDouble(odds[6]), toDouble(odds[7]), toDouble(odds[8]));
    }

    public Double toDouble(String oddInString) {
        oddInString = oddInString.replaceAll(",", ".");
        return oddInString.equals("-") ? 0.0 : Double.valueOf(oddInString);
    }

    public TeamMatchHistory scrapeTeamHistory(String detailUrl, String teamName, Odds odds) {
        if (detailUrl == null || detailUrl.isEmpty()) return null;
        List<String> names = scrapeDetailUrl(detailUrl);
        if (names.size() < 3) {
            System.out.println("⚠️ Takım isimleri çekilemedi, atlanıyor: " + detailUrl);
            return null;
        }

        TeamMatchHistory teamHistory = new TeamMatchHistory(names.get(0), names.get(1), names.get(2), detailUrl, odds);
        try {
            List<MatchResult> rekabetGecmisi = scrapeRekabetGecmisi(detailUrl + "/rekabet-gecmisi");
            rekabetGecmisi.forEach(teamHistory::addRekabetGecmisiMatch);

            List<MatchResult> sonMaclarHome = scrapeSonMaclar(detailUrl + "/son-maclari", 1);
            sonMaclarHome.forEach(m -> teamHistory.addSonMacMatch(m, 1));

            List<MatchResult> sonMaclarAway = scrapeSonMaclar(detailUrl + "/son-maclari", 2);
            sonMaclarAway.forEach(m -> teamHistory.addSonMacMatch(m, 2));

        } catch (Exception e) {
            System.out.println("Takım geçmişi çekme hatası: " + e.getMessage());
        }
        return teamHistory;
    }

    private List<String> scrapeDetailUrl(String url) {
        List<String> names = new ArrayList<>();
        try {
            driver.get(url);
            PageWaitUtils.safeWaitForLoad(driver, 10);

            List<WebElement> teamLinks = driver.findElements(By.cssSelector("a[data-test-id='TeamLink'] span[data-test-id='HeaderTeams']"));
            String homeTeam = teamLinks.size() > 0 ? teamLinks.get(0).getText().trim() : "-";
            String awayTeam = teamLinks.size() > 1 ? teamLinks.get(1).getText().trim() : "-";

            names.add(homeTeam + " - " + awayTeam);
            names.add(homeTeam);
            names.add(awayTeam);
        } catch (Exception e) {
            System.out.println("Rekabet geçmişi hatası: " + e.getMessage());
        }
        return names;
    }

    private List<MatchResult> scrapeRekabetGecmisi(String url) {
        List<MatchResult> matches = new ArrayList<>();
        try {
            driver.get(url);
            PageWaitUtils.safeWaitForLoad(driver, 10);

            if (!PageWaitUtils.elementExists(driver, By.cssSelector("div[data-test-id='CompitionHistoryTable']"), 5)) {
                return matches;
            }

            selectTournament();
        } catch (Exception e) {
            System.out.println("Rekabet geçmişi hatası: " + e.getMessage());
        }
        return matches;
    }

    private List<MatchResult> scrapeSonMaclar(String url, int homeOrAway) {
        List<MatchResult> matches = new ArrayList<>();
        try {
            driver.get(url);
            PageWaitUtils.safeWaitForLoad(driver, 10);

            String selector = (homeOrAway == 1)
                    ? "div[data-test-id='LastMatchesTableFirst'] table"
                    : "div[data-test-id='LastMatchesTableSecond'] table";

            if (!PageWaitUtils.elementExists(driver, By.cssSelector(selector), 8)) {
                System.out.println("⚠️ Tablo bulunamadı: " + selector);
                return Collections.emptyList();
            }

            selectTournament();
        } catch (Exception e) {
            System.out.println("Son maçlar hatası: " + e.getMessage());
        }
        return matches;
    }

    private void selectTournament() {
        try {
            PageWaitUtils.safeClick(driver, By.cssSelector("div[data-test-id='CustomDropdown']"), 6);
            PageWaitUtils.safeClick(driver, By.xpath("//div[@role='option']//span[contains(text(), 'Bu Turnuva')]"), 6);
            PageWaitUtils.safeWaitForLoad(driver, 5);
        } catch (Exception e) {
            System.out.println("Turnuva seçimi atlandı: " + e.getClass().getSimpleName());
        }
    }

    public void close() {
        try {
            if (driver != null) driver.quit();
        } catch (Exception ignore) {}
    }
}
