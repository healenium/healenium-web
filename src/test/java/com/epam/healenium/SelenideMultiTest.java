package com.epam.healenium;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.WebDriverProvider;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.time.LocalDateTime;

import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

@Slf4j
public class SelenideMultiTest {

    private static final int PORT = 8090;

    @ClassRule
    public static TestServer server = new TestServer("SomeNewRoot", PORT);

    @BeforeClass
    public static void setUp() {
        Configuration.browser = MyGridProvider.class.getName();
    }

    @Before
    public void before() {
        log.info("START before at {}", LocalDateTime.now());
        open(String.format("http://localhost:%d", PORT));
        $(By.id("user_name")).click();
        log.info("COMPLETE before at {}", LocalDateTime.now());
    }

    @Test
    public void userNameTest() {
        log.info("START userNameTest at {}", LocalDateTime.now());
        $(By.id("user_name")).setValue("erin");
        $(By.id("user_name")).shouldHave(value("erin"));
        log.info("COMPLETE userNameTest at {}", LocalDateTime.now());
    }

    @Test
    public void passwordTest() {
        log.info("START passwordTest at {}", LocalDateTime.now());
        $(By.id("password")).setValue("secret");
        $(By.id("password")).shouldHave(value("secret"));
        log.info("COMPLETE passwordTest at {}", LocalDateTime.now());
    }

    @After
    public void after() {
        log.info("START after at {}", LocalDateTime.now());
        $(By.xpath("//div/button[@type='submit']")).click();
        log.info("COMPLETE after at {}", LocalDateTime.now());
    }

    private static class MyGridProvider implements WebDriverProvider {
        @Override
        public WebDriver createDriver(DesiredCapabilities capabilities) {
            Configuration.timeout = 15000;

            WebDriverManager.chromedriver().setup();
            capabilities.setBrowserName("chrome");
            ChromeOptions options = new ChromeOptions();
            options.setHeadless(true);
            options.merge(capabilities);
            WebDriver wd = new ChromeDriver(options);
            return SelfHealingDriver.create(wd);
        }
    }

}
