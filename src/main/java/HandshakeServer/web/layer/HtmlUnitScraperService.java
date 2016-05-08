package HandshakeServer.web.layer;

import HandshakeServer.Neo4jDb.Person;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by hleb on 4/14/16.
 */
public class HtmlUnitScraperService implements ScraperServiceInterface {

    private String personId;
    private WebClient client;
    private List<Person> friends;
    private HtmlPage page;
    private Document doc;

    public void simulateBrowser() {
        BrowserVersion browserVersion = new BrowserVersion("Opera Mini", "5.1", "Opera/9.80 (Series 60; Opera Mini/5.1.22783/23.334; U; en) Presto/2.5.25 Version/10.54", 22783);
        client = new WebClient(browserVersion);
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setRedirectEnabled(true);
        client.getOptions().setAppletEnabled(false);
        client.getOptions().setCssEnabled(false);
    }

    public void authorize() throws  IOException{
        // find all needed elements for logging in, fill them and submit
        System.out.println("authorizing");

        page = client.getPage("https://m.facebook.com/");

        HtmlTextInput email = (HtmlTextInput) page.getByXPath("//*[@name='email']").get(0);
        HtmlPasswordInput password = (HtmlPasswordInput) page.getByXPath("//*[@name='pass']").get(0);
        final HtmlButton button = (HtmlButton) page.getByXPath("//*[@type='submit']").get(0);

        email.setValueAttribute(Props.EMAIL);
        password.setValueAttribute(Props.PASSWORD);

        page = button.click();
    }

        private void scrollForFriends() throws IOException {

            page = client.getPage("https://m.facebook.com/profile.php?v=friends&id=" + personId + "&refid=17");

            /*try (PrintWriter out = new PrintWriter("filename.html")) {
                out.println(page.getWebResponse().getContentAsString());
            }*/

            double start = System.currentTimeMillis();

            while (true) {
                doc = Jsoup.parse(page.getWebResponse().getContentAsString());
                parseForFriends();


                Elements elems = doc.getElementsByAttributeValue("id", "m_more_friends");
                if (elems.size() < 1) {
                    break;
                }

                Element e = elems.get(0).child(0);
                String newUrl = "https://m.facebook.com" + e.attr("href");

                page = client.getPage(newUrl);
                //System.out.println(System.currentTimeMillis() - start + " overall");
                client.close();
            }
        }

    private void parseForFriends() {
        Elements e = doc.getElementsByAttributeValue("class", "_5pxc");
        for (int i = 0; i < e.size(); i++) {
            friends.add(new Person("mock", e.get(0).text()));
        }
    }

    public void closeSession() {
        System.out.println("Closing PhantomJs session");
    }

    public List<Person> getFriendsList(String personId) {
        this.personId = personId;
        friends = new ArrayList<>();

        try {
            scrollForFriends();
        } catch (IOException ex) {}

        return friends;
    }
}
