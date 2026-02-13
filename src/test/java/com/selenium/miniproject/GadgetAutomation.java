package com.selenium.miniproject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.time.Duration;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.ITestResult;
import org.testng.annotations.*;
import org.testng.asserts.SoftAssert;

public class GadgetAutomation {

    private WebDriver driver;
    private WebDriverWait wait;
    private JavascriptExecutor js;

    // ---------- Driver Setup ----------
    @Parameters("browser")
    @BeforeClass
    public void setUp(@Optional("chrome") String browser) {
        if (browser.equalsIgnoreCase("chrome")) {
            driver = new ChromeDriver();
        } else if (browser.equalsIgnoreCase("firefox")) {
            driver = new FirefoxDriver();
        } else if (browser.equalsIgnoreCase("edge")) {
            driver = new EdgeDriver();
        } else {
            throw new IllegalArgumentException("Unsupported browser: " + browser);
        }
        driver.manage().window().maximize();
        driver.manage().deleteAllCookies();
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        js = (JavascriptExecutor) driver;
    }

    // ---------- Screenshot Utility ----------
    public void takeScreenshot(String fileName) {
        try {
            // Format timestamp as yyyy-MM-dd_HH-mm-ss
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HHmmss").format(new Date());
            TakesScreenshot ts = (TakesScreenshot) driver;
            File src = ts.getScreenshotAs(OutputType.FILE);
            File dest = new File(System.getProperty("user.dir") + "/screenshots/"
                    + fileName + "_" + timestamp + ".png");
            FileUtils.copyFile(src, dest);
            System.out.println("Screenshot saved: " + dest.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ---------- Search ----------
    public void search(String query) {
        By searchBox = By.id("search-box-input");
        wait.until(ExpectedConditions.visibilityOfElementLocated(searchBox))
                .sendKeys(query + Keys.ENTER);
        takeScreenshot("search_results");
    }

    // ---------- Results ----------
    public void sortByPopularity() {
        WebElement sortTrigger = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("div.sort-drop")));
        sortTrigger.click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("ul.sort-value")));
        WebElement popularity = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//li[@data-sorttype='plrty']")));
        js.executeScript("arguments[0].click();", popularity);
        takeScreenshot("sorted_by_popularity");
    }

    public void setPriceRange(String min, String max) {
        WebElement fromVal = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("fromVal")));
        fromVal.clear();
        fromVal.sendKeys(min);

        WebElement toVal = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("toVal")));
        toVal.clear();
        toVal.sendKeys(max);

        WebElement arrow = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//div[contains(@class,'price-go-arrow')]")));
        arrow.click();

        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("div.product-tuple-listing")));
        takeScreenshot("filtered_by_price");
    }

    public void printTopProducts(int count, int minPrice, int maxPrice) {
        SoftAssert softAssert = new SoftAssert();

        FluentWait<WebDriver> fluentWait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(30))
                .pollingEvery(Duration.ofSeconds(5))
                .ignoring(StaleElementReferenceException.class);

        By productsBy = By.cssSelector("div.product-tuple-listing");
        List<WebElement> products = fluentWait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(productsBy));

        int printed = 0;
        for (int i = 0; i < products.size() && printed < count; i++) {
            try {
                WebElement product = driver.findElements(productsBy).get(i);

                String name = product.findElement(By.cssSelector("p.product-title")).getText().trim();
                String priceText = product.findElement(By.cssSelector("span.product-price")).getText().trim();
                int priceVal = Integer.parseInt(priceText.replaceAll("[^0-9]", ""));

                if (priceVal >= minPrice && priceVal <= maxPrice) {
                    printed++;
                    System.out.println(printed + ". " + name + " - Rs. " + priceVal);
                    softAssert.assertTrue(priceVal >= minPrice && priceVal <= maxPrice,
                            "Price not in expected range for product: " + name);
                }
            } catch (StaleElementReferenceException e) {
                System.out.println("Stale element at index " + i + ", retrying...");
                i--;
            }
        }
        takeScreenshot("top_products");

        softAssert.assertAll();
    }

    // ---------- TestNG Flow ----------
    @Test(priority = 1)
    public void openSnapdeal() {
        SoftAssert softAssert = new SoftAssert();
        driver.get("https://www.snapdeal.com/");
        System.out.println("Page title: " + driver.getTitle());
        System.out.println("Page URL: " + driver.getCurrentUrl());
        softAssert.assertTrue(driver.getCurrentUrl().contains("snapdeal.com"),
                "Snapdeal homepage not loaded! Current URL: " + driver.getCurrentUrl());
        takeScreenshot("homepage");
        softAssert.assertAll();
    }

    @Test(priority = 2)
    public void searchProduct() {
        search("Bluetooth headphone");
    }

    @Test(priority = 3)
    public void sortResults() {
        sortByPopularity();
    }

    @Test(priority = 4)
    public void filterByPrice() {
        setPriceRange("700", "1400");
    }

    @Test(priority = 5)
    public void printTopFiveProducts() {
        printTopProducts(5, 700, 1400);
    }

    // ---------- Tear Down ----------
    @AfterMethod
    public void captureFailureScreenshot(ITestResult result) {
        if (ITestResult.FAILURE == result.getStatus()) {
            takeScreenshot("FAILED_" + result.getName());
        }
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
