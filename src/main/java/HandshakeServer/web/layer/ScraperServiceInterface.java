package HandshakeServer.web.layer;


import HandshakeServer.Neo4jDb.Person;

import java.util.List;

/**
 * Created by hleb on 3/30/16.
 */
public interface ScraperServiceInterface {
    List<Person> getFriendsList(String personId);

    void closeSession();
}
