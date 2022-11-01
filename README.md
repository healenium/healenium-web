# healenium-web
Self-healing library for Selenium Web-based tests

[![Maven Central](https://img.shields.io/maven-central/v/com.epam.healenium/healenium-web.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.epam.healenium%22%20AND%20a:%22healenium-web%22)
 [![@healenium](https://img.shields.io/badge/Telegram-%40healenium-orange.svg)](https://t.me/healenium)<br />
⇧ Join us! ⇧
## How to start

### 0. Start hlm-backend by [instruction](https://github.com/healenium/healenium-backend)

### 0.1 Add dependency

for Gradle projects:
``` 
dependencies {
    compile group: 'com.epam.healenium', name: 'healenium-web', version: '3.3.1'
}
```

for Maven projects:
``` 
<dependency>
	<groupId>com.epam.healenium</groupId>
	<artifactId>healenium-web</artifactId>
	<version>3.3.1</version>
</dependency>
```
### 1. Init driver instance of SelfHealingDriver
``` 
//declare delegate
WebDriver delegate = new ChromeDriver();
//create Self-healing driver
SelfHealingDriver driver = SelfHealingDriver.create(delegate);
 ```
### 2. Specify custom healing config file healenium.properties under test/resources directory, ex.:
``` 
recovery-tries = 1
score-cap = 0.5
heal-enabled = true
hlm.server.url = http://localhost:7878
hlm.imitator.url = http://localhost:8000
 ```
> recovery-tries - list of proposed healed locators

> heal-enabled - flag to enable or disable healing.
Also you can set this value via -D or System properties, for example to turn off healing for current test run: -Dheal-enabled=false

> score-cap - score value to enable healing with predefined probability of match (0.5 means that healing will be performed for new healed locators where probability of match with target one is >=50% )

> hlm.server.url - ip:port or name where hlm-backend instance is installed

> hlm.imitator.url - ip:port or name where imitate instance is installed

### 3. Simply use standard By/@FindBy to locate your elements
```
@FindBy(xpath = "//button[@type='submit']")
private WebElement testButton;
...
public void clickTestButton() {
     driver.findElement(By.cssSelector(".test-button")).click();
}
```
### 4. To disable healing for some element you can use @DisableHealing annotation over the method where element is called. Ex: If you want to verify that element is not present on the page.
```
@DisableHealing
public boolean isButtonPresent() {
    try {
        return driver.findElement(By.cssSelector(".test-button")).isDisplayed();
    } catch (NoSuchElementException e) {
        return false;
    }
}
```
### 5. Add [hlm-report-gradle](https://github.com/healenium/healenium-report-gradle) or [hlm-report-mvn](https://github.com/healenium/healenium-report-mvn) plugin to enable reporting
### 6. Add [hlm-idea](https://github.com/healenium/healenium-idea) plugin to enable locator updates in your TAF code
### 7. Run tests as usual using Maven mvn clean test or Gradle ./gradlew clean test
