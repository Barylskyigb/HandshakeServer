package HandshakeServer.web.layer;

import HandshakeServer.Neo4jDb.Person;
import HandshakeServer.Main;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by hleb on 4/1/16.
 *
 * This singleton is going to be used to scrape friend list from facebook.
 * The reason i'm scraping info is the inability of getting friend list of any person using Graph API.
 *
 * In case of using Graph API, application would need to get user_friends permission from BOTH person
 * who is using application and person who would have to be showed in friend lists.
 * Information of someone's friend list is public(mostly) and
 * the only way I see it possible at the moment to get this info is to scrape it from html.
 * I'm using Selenium with JsPhantomDriver and JSoup library to scrape(parsing with Selenium was pretty slow).
 *
 *
 * todo multithreading
 *
 * P.S. it's needed to have class HandshakeServer.web.layer.Props with EMAIL and PASSWORD constants
 * for facebook profile which will be used by crawler
 *
 *
 *
 *
 * ****************************
 * I've wrote a phantomjs script which is used instead of this class and maybe will be added to github later
 *
 */
public class PhantomJsScraperService implements ScraperServiceInterface {

    private String personId;
    private WebDriver driver;
    private List<Person> friends;

    public void simulateBrowser() {
        //create capabilities object to set up PhantomJs
        //and change some settings to boost performance a bit
        DesiredCapabilities capabilities = new DesiredCapabilities();
        findPhantomJsDriver(capabilities);
        //capabilities.setCapability("phantomjs.page.settings.loadImages", false); //causes huge climb of RAM usage
        capabilities.setCapability("disk-cache", false); //todo

        capabilities.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, new String[] {"--webdriver-loglevel=ERROR"});
        driver = new PhantomJSDriver(capabilities);
    }

    public void authorize() {
        // find all needed elements for logging in, fill them and submit
        driver.get("https://www.facebook.com");

        try {
            WebElement button = driver.findElement(By.xpath("//input[@type='submit']"));
            WebElement login = driver.findElement(By.name("email"));
            WebElement pass = driver.findElement(By.name("pass"));

            login.sendKeys(Props.EMAIL); //todo more security
            pass.sendKeys(Props.PASSWORD);

            button.click();
        } catch (NoSuchElementException ex) {ex.printStackTrace(); driver.quit(); authorize();} //todo figure out why this expr is happning
    }

    private void scrollForFriends() throws WebDriverException {
        //It's needed to scroll down on friends page to get all the people loaded to html

        //driver.switchTo().defaultContent();

        String url = "https://www.facebook.com/" + personId + "/friends";
        driver.get(url);
        JavascriptExecutor js = (JavascriptExecutor)driver;
        js.executeScript("localStorage.clear()"); //doesn't help

        //parse to check how many people's loaded.
        //if there's no one else to load (for example, photos are showing up), move on
        //String xpath = "//div[@id='timeline-medley']/div/div[2]";
        Document doc;
        int iterations = 0;
        int elementsCountBefore = 0;
        int elementsCountNow;

        while (true){ //todo performance
            js.executeScript("window.scrollBy(0,3000)");

            doc = Jsoup.parse(driver.getPageSource());
            elementsCountNow = doc.getElementsByClass("fsl").size();

            if (elementsCountNow > elementsCountBefore) {
                elementsCountBefore = elementsCountNow;
                iterations = 0;
            }

            if (iterations == 5) {
                break;
            }

            iterations++;
        }
    }

    private void parseForFriends() {
        //parse (JSoup) the html to get friends names and ids (from an element which contains that info)
        //and create objects of Person class, add objects to friends list

        friends = new ArrayList<>();
        Document doc = Jsoup.parse(driver.getPageSource());
        Elements elements = doc.getElementsByAttributeValueContaining("class", "fsl fwb fcb");
        Element e;
        String id;
        String name;

        for (int i = 0; i < elements.size(); i++) {
            try {
                //get <a> tag, which contains person's name and id
                e = elements.get(i).getElementsByTag("a").get(0);
                String attrWithId = e.attr("data-hovercard");
                id = attrWithId.substring(attrWithId.indexOf("id=")+3, attrWithId.indexOf("&")); //id can be found inside data-hovercard attribute in id parameter
                name = e.text();
                friends.add(new Person(id, name));
            } catch (IndexOutOfBoundsException ex) {
                System.out.println(personId);
                ex.printStackTrace();} //todo handle it so that it will not throw expr
        }

        //driver.close();
    }

    public List<Person> getFriendsList(String personId) {
        this.personId = personId;
        try {
            scrollForFriends();
            parseForFriends();
        } catch (WebDriverException ex) {
            ex.printStackTrace();
            //restart
            simulateBrowser();
            authorize();

            return getFriendsList(personId);
        }

        return friends;
    }

    public void closeSession() {
        System.out.println("Closing PhantomJs session");
        driver.quit(); //todo throws exceptions
    }

    private void findPhantomJsDriver(DesiredCapabilities capabilities) {
        /**
         * Part of this method I've found on Stack Overflow and rewrote here
         * http://stackoverflow.com/questions/15359702/get-location-of-jar-file
         */

        File file;
        URL url;
        String extURL; //  url.toExternalForm();

        try {
            url = Main.class.getProtectionDomain().getCodeSource().getLocation();
            // url is in one of two forms?
            //        ./build/classes/   NetBeans test
            //        jardir/MyJar.jar  froma jar
        } catch (SecurityException ex) {
            url = Main.class.getResource(Main.class.getSimpleName() + ".class");
            // url is in one of two forms, both ending "/com/physpics/tools/ui/PropNode.class"
            //          file:/C:/User/PathToProject/build/classes
            //          jar:file:/C:/User/pathToProject/target/MyJar.jar!
        }

        // convert to external form
        extURL = url.toExternalForm();

        // prune for various cases
        if (extURL.endsWith(".jar")) { // from getCodeSource
            extURL = extURL.substring(0, extURL.lastIndexOf("/"));
        }
        else {  // from getResource
            String suffix = "/"+(Main.class.getName()).replace(".", "/")+".class";
            extURL = extURL.replace(suffix, "");
            if (extURL.startsWith("jar:") && extURL.endsWith(".jar!/")) {
                extURL = extURL.substring(4, extURL.lastIndexOf("/"));
                //cut jarfile's name
                extURL = extURL.substring(0, extURL.lastIndexOf("/"));
            }
        }

        // convert back to url
        try {
            url = new URL(extURL);
        } catch (MalformedURLException mux) {
            mux.printStackTrace();
            // leave url unchanged; probably does not happen
        }

        // convert url to File
        try {
            file = new File(url.toURI());
        } catch(URISyntaxException ex) {
            file = new File(url.getPath());
        }


        String jarDir = file.getPath();

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            capabilities.setCapability("phantomjs.binary.path",
                    jarDir + "\\phantomjs-2.1.1-windows\\bin\\phantomjs.exe");
        }
        else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            capabilities.setCapability("phantomjs.binary.path",
                    jarDir + "/phantomjs-2.1.1-linux-x86_64/bin/phantomjs");
        }
        else {
            System.out.println("Operating system is not supported");
            System.exit(1);
        }
    }
}
