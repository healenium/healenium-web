# healenium-web
Self-healing library for Selenium Web-based tests


[ ![Download](https://api.bintray.com/packages/epam/healenium/healenium-web/images/download.svg) ](https://bintray.com/epam/healenium/healenium-web/_latestVersion)
[![Coverage Status](https://coveralls.io/repos/github/healenium/healenium-web/badge.svg)](https://coveralls.io/github/healenium/healenium-web)
[![Build Status](https://github.com/healenium/healenium-web/workflows/Java-CI-test/badge.svg)](https://github.com/healenium/healenium-web/workflows/Java-CI-test/badge.svg)

## How to start

### 0. Add dependency 
for Gradle projects:
``` 
repositories {
    maven {
        url  "https://dl.bintray.com/epam/healenium"
    }
    mavenCentral()
}
dependencies {
    compile group: 'com.epam.healenium', name: 'healenium-web', version: '2.0'
}
```

for Maven projects:
``` 
<repositories>
     <repository>
        <snapshots>
          <enabled>false</enabled>
        </snapshots>
        <id>bintray-epam-healenium</id>
        <name>bintray</name>
        <url>https://dl.bintray.com/epam/healenium</url>
     </repository>
</repositories>

<dependency>
	<groupId>com.epam.healenium</groupId>
	<artifactId>healenium-web</artifactId>
	<version>2.0.2</version>
</dependency>
```

### 1. Driver initialization
### 1.1 Init driver instance of SelfHealingDriver with custom config:
``` //declare delegate
            WebDriver delegate = new ChromeDriver();
            //declare configs for sha
            Config config = ConfigFactory.load("sha.properties");
            //create sha driver
            SelfHealingDriver driver = SelfHealingDriver.create(delegate, config);
 ```

### 1.2 Or Init driver instance of SelfHealingDriver with default config:
``` //declare delegate
            WebDriver delegate = new ChromeDriver();
            //create sha driver
            SelfHealingDriver driver = SelfHealingDriver.create(delegate);
```
### 1.2.1 Default config values:
``` recovery-tries = 3
    basePath = sha/healenium
    reportPath = build/reports
    screenshotPath = build/screenshots/
    heal-enabled = true
 ```

 > recovery-tries - list of proposed healed locators

 > basePath - folder to store base locators path

 > **Important!** Do not delete data from the folder where files with new locators are stored. They are used to perform self-healing in next automation runs

 > reportPath - folder to save test report with healing information

 > screenshotPath - folder to save screenshots of healed elements

 > heal-enabled - you could enable or disable healing by setting true or false flag to this variable

* Suggested way is to declare custom config or property file (ex. sha.properties) and set
``` basePath = sha/selenium```

Also you could set configs via -D or System properties, for example to turn off healing for current test run:
```-Dheal-enabled=false```

### 2. Locating elements
### Simply use standard By/@FindBy to locate your elements
![#f03c15](https://placehold.it/15/f03c15/000000?text=+) From version **2.0.2** Healium supports either standart **By/@FindBy** or **PageAwareBy/@PageAwareFindBy** usage for healing.

![#1589F0](https://placehold.it/15/1589F0/000000?text=+) In **2.0.1** and earlier healing will work only for elements that are declared using **PageAwareBy/@PageAwareFindBy**

#### Using PageAwareBy.by instead of By to locate your elements in 2.0.1 and earlier
```By buttonBy = PageAwareBy.by("MainPage", By.id(testButtonId));```

* where the first argument "MainPage" is the name of the page to which the WebElement belongs.

Then you can simply call findElement() method as usual
``` driver.findElement(buttonBy).click(); ```

Or use the shorter form
```driver.findElement(PageAwareBy.by("MainPage", By.id(testButtonId))).click();```

#### Using @PageAwareFindBy instead of @FindBy to locate your elements in 2.0.1 and earlier

```
    @PageAwareFindBy(page="MainPage", findBy = @FindBy(id = "markup-generation-button"))
    WebElement testButtonId;
```

or not declaring the page. In this case page name will be set by default with the class name in which the locator is declared.

 ```
    @PageAwareFindBy(findBy = @FindBy(id = "markup-generation-button"))
    WebElement testButtonId;
 ```

#### To refactor your project fast you could use Idea hotkeys cmd+shift+r and perform replacement
