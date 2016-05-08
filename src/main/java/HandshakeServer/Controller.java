package HandshakeServer;

import HandshakeServer.Neo4jDb.Person;
import HandshakeServer.Neo4jDb.Neo4jService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by hleb on 5/7/16.
 */

@RestController
public class Controller {

    @Autowired
    ApplicationContext context;
    @Autowired
    Neo4jService service;

    @RequestMapping("/people/{facebookId}")
    public List<Person> getPerson(@PathVariable("facebookId") String facebookId) {
        return service.getFriendsList(facebookId);
    }

    @RequestMapping("/path/{facebookId1}-{facebookId2}")
    public List<Person> getShortestPaths(@PathVariable("facebookId1") String facebookId1,
                                         @PathVariable("facebookId2") String facebookId2)
    {
        List<List<Person>> paths = service.getPaths(facebookId1, facebookId2);
        return paths.get(0);
    }

}
