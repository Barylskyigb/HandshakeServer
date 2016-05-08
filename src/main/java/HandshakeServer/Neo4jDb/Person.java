package HandshakeServer.Neo4jDb;

import java.util.List;

/**
 * Created by hleb on 3/3/16.
 *
 */

public class Person {

    //@Autowired
    //private ScraperServiceInterface scraper;
    private long updateDate;
    private String name;
    private String facebookId;
    private List<Person> friends;

    public long getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(long updateDate) {
        this.updateDate = updateDate;
    }

    public String getName() {
        return name;
    }

    public Person() {}

    public Person(String facebookId) {
        this.facebookId = facebookId;
    }

    public Person(String facebookId, String name) {
        this.facebookId = facebookId;
        this.name = name;
    }

    private void findFriends() {
        //friends = scraper.getFriendsList(this.facebookId);
    }

    public List<Person> getFriends() { //todo multithreading
        if (friends == null) {
            findFriends();
        }

        return friends;
    }

    public String getFacebookId() {
        return facebookId;
    }

    public void setFriends(List<Person> friends) {
        this.friends = friends;
    }
}
