package com.basketbol.scraper;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;

public class PageWaitUtils {

    public static void safeWaitForLoad(WebDriver driver, int timeoutSeconds) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));

        try {
            wait.until(d -> ((JavascriptExecutor) d)
                    .executeScript("return document.readyState").equals("complete"));
        } catch (Exception ignored) {}

        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        } catch (Exception ignored) {}
    }

    public static boolean elementExists(WebDriver driver, By locator, int timeoutSeconds) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
                    .until(ExpectedConditions.presenceOfElementLocated(locator));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    public static boolean safeClick(WebDriver driver, By locator, int timeoutSeconds) {
        try {
            WebElement el = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
                    .until(ExpectedConditions.elementToBeClickable(locator));
            el.click();
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Element tıklanamadı: " + locator);
            return false;
        }
    }
}
