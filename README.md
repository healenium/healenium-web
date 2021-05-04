# healenium-web
Self-healing library for Selenium Web-based tests

[![Coverage Status](https://coveralls.io/repos/github/healenium/healenium-web/badge.svg)](https://coveralls.io/github/healenium/healenium-web)
[![Build Status](https://github.com/healenium/healenium-web/workflows/Java-CI-test/badge.svg)](https://github.com/healenium/healenium-web/workflows/Java-CI-test/badge.svg)

## How to start

### 0. For version 3.0 and higher start hlm-backend by [instruction](https://github.com/healenium/healenium-backend) 

### 0.1 Add dependency 

for Gradle projects:
``` 
dependencies {
    compile group: 'com.epam.healenium', name: 'healenium-web', version: '3.0.3-beta-8'
}
```

for Maven projects:
``` 
<dependency>
	<groupId>com.epam.healenium</groupId>
	<artifactId>healenium-web</artifactId>
	<version>3.0.3-beta-8</version>
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
serverHost = localhost
serverPort = 7878
 ```
 > recovery-tries - list of proposed healed locators
 
 > heal-enabled - flag to enable or disable healing. 
Also you can set this value via -D or System properties, for example to turn off healing for current test run: -Dheal-enabled=false

 > score-cap - score value to enable healing with predefined probability of match (0.5 means that healing will be performed for new healed locators where probability of match with target one is >=50% )
 
 > serverHost - ip or name where hlm-backend instance is installed
 
 > serverPort - port on which hlm-backend instance is installed (7878 by default)

### 3. Simply use standard By/@FindBy to locate your elements
To disable healing for some element you can use @DisableHealing annotation over the method where element is called
```
@DisableHealing
public void clickTestButton() {
     testButton.click();
}
```
### 4. Add [hlm-report-gradle](https://github.com/healenium/healenium-report-gradle) or [hlm-report-mvn](https://github.com/healenium/healenium-report-mvn) plugin to enable reporting
### 5. Add [hlm-idea](https://github.com/healenium/healenium-idea) plugin to enable locator updates in your TAF code
### 6. Run tests as usual using Maven mvn clean test or Gradle ./gradlew clean test

### ![#f03c15](https://placehold.it/15/f03c15/000000?text=+)!Note versions 2.0.5 and earlier are not supported anymore
For versions 2.0.5 and earlier file storage is used and no hlm-backend is needed. If you want to use earliest versions for some reasons follow this instruction:
1. Driver initialization
1.1 Init driver instance of SelfHealingDriver with custom config:
``` //declare delegate
            WebDriver delegate = new ChromeDriver();
            //declare configs for sha
            Config config = ConfigFactory.load("sha.properties");
            //create sha driver
            SelfHealingDriver driver = SelfHealingDriver.create(delegate, config);
 ```

1.2 Or Init driver instance of SelfHealingDriver with default config:
``` //declare delegate
            WebDriver delegate = new ChromeDriver();
            //create sha driver
            SelfHealingDriver driver = SelfHealingDriver.create(delegate);
```
1.2.1 Default config values:
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

2. Locating elements
Simply use standard By/@FindBy to locate your elements

