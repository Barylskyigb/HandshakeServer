package HandshakeServer.Neo4jDb;

import org.apache.log4j.Logger;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.UniqueFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.io.*;
import java.nio.file.*;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by hleb on 4/9/16.
 *
 * With usage of Neo4jRepository, as far as I understand, transactions are being created by one
 * for each object saved to database. Didn't found any solution except using EmbeddedDriver and
 * manually created class (Neo4jService). Looks like it's not possible to use
 * Spring Data Neo4j 4 now (at least for saving nodes).
 *
 *
 * todo comment
 *
 */

public class Neo4jService {

    private static final int MAX_DEPTH = 8;
    private final Path IDS_PATH = FileSystems.getDefault().getPath("Ids.txt");
    public static final long LAST_UPDATE_DATE = 1;

    private List<String> fileLines = new ArrayList<>(3);
    private int lastAppId; //this is the id to start with next time app will be looking for people to scan
    private int lastId; //amount of nodes in db

    @Autowired private UniqueFactory.UniqueNodeFactory factory;
    @Autowired private Logger logger;
    @Autowired private GraphDatabaseService graphDatabaseService;
    @Autowired private ApplicationContext applicationContext;

    Neo4jService() {}

    public void save(List<Person> people) { //inject a list of scanned people ready to be saved in database

        double start = System.currentTimeMillis();
        lastId = Integer.valueOf(readLineFromFile(0));

        try (Transaction tx = graphDatabaseService.beginTx()){

            for (int i = 0; i < people.size(); i++) {
                createOrUpdate(people.get(i));
            }

            tx.success();
        }
        writeLastId(lastAppId);//todo place to method which will stop program
        replaceLineInFile(0, lastId);

        double end = System.currentTimeMillis();
        logger.info("Transaction time: " + (end - start));
    }

    private void createOrUpdate(Person person) { //todo если программа прервётся в этом методе, несколько человек могут не просканироваться
        List<Person> friends = person.getFriends();
        Node friendNode;

        Node personNode = factory.getOrCreate("facebookId", person.getFacebookId());

        if (person.getName() != null) {
            personNode.setProperty("name", person.getName()); }
        personNode.setProperty("updateDate", person.getUpdateDate());

        for (int i = 0; i < friends.size(); i++) {
            friendNode = factory.getOrCreate("facebookId", friends.get(i).getFacebookId()); //todo

            if (friendNode.hasProperty("appId")) { //if person is present in db
                friendNode.setProperty("name", friends.get(i).getName());
            //if (true)
                personNode.createRelationshipTo(friendNode, Relations.FRIEND_TO); //todo how to make bidirectional?
            }
            else {
                lastId++;

                friendNode.setProperty("appId", lastId); //move to new class Generator
                friendNode.setProperty("updateDate", 0); //todo currentTime
                friendNode.setProperty("name", friends.get(i).getName());
                personNode.createRelationshipTo(friendNode, Relations.FRIEND_TO);
            }
        }
    }

    public List<Person> fetchPeopleToScan(int peopleCount) {
        lastAppId = readLastAppId();

        Logger.getRootLogger().info("Looking for not scanned people in db");
        int iterations = 0;
        Node node;
        List<Node> nodes = new ArrayList<>(peopleCount); //todo nodes.size isn't equal to peopleCount
        List<Person> persons;

        try (Transaction tx = graphDatabaseService.beginTx()) {
            while (nodes.size() < peopleCount){
                node = graphDatabaseService.findNode(DynamicLabel.label("Person"), "appId", iterations + lastAppId);

                if (node == null) {
                    break;
                }

                if (Integer.valueOf(node.getProperty("updateDate") + "") < 1) { // < [specific point in time] //todo move to props file
                    nodes.add(node);
                }

                iterations++;
            }

            persons = convertList(nodes);
            tx.success();
            Logger.getRootLogger().info("People found");
        }

        lastAppId += iterations;


        /*List<Person> populated = new ArrayList<>();
        populated.add(context.getBean(Person.class, "100004343261791", "Hleb Be"));
        return populated;*/
        return persons;
    }

    private List<Person> convertList(List<Node> nodes){ //todo Node->N (generic)
        List<Person> persons = new ArrayList<>();

        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            Person p = applicationContext.getBean(Person.class,  nodes.get(i).getProperty("facebookId").toString(),
                    nodes.get(i).getProperty("name").toString());
            p.setUpdateDate(Integer.valueOf(n.getProperty("updateDate")+""));

            persons.add(p);
        }

        return persons;
    }

    private <T> List<T> iteratorToList(Iterator<T> iterator) {
        List<T> list = new ArrayList<>();

        while (iterator.hasNext()) { //convert iterator to list
            list.add(iterator.next());
        }

        return list;
    }

    private void writeLastId(int lastId) {
        replaceLineInFile(1, lastId);
    }

    private int readLastAppId() {
        return Integer.valueOf(readLineFromFile(1));
    }

    private String readLineFromFile(int lineNum) {
        try {
            fileLines = Files.readAllLines(IDS_PATH);
        } catch (IOException ex) {}

        return fileLines.get(lineNum);
    }

    private void  replaceLineInFile(int lineNum, int replacement) {
        //todo replace with smth
        fileLines.add(null);
        fileLines.add(null);

        String fileContent = "";
        fileLines.set(lineNum, replacement + "");

        for (int i = 0; i < fileLines.size(); i++) {
            fileContent += fileLines.get(i) + '\n';
        }

        try (FileOutputStream fileOut = new FileOutputStream(IDS_PATH.toString())) {
            fileOut.write(fileContent.getBytes());
        } catch (IOException ex) {ex.printStackTrace();}
    }

    public void createStartNode() {
        try (Transaction tx = graphDatabaseService.beginTx()) {
            Node mainNode = factory.getOrCreate("facebookId", "100007631628945");
            mainNode.setProperty("name", "Hleb Be");
            mainNode.setProperty("updateDate", 0);
            mainNode.setProperty("appId", 0);

            tx.success();
        }

        replaceLineInFile(0, 0);
        replaceLineInFile(1, 0);
    }

    public List<Person> getPeople(PeopleType type) {
        List<Person> people;
        try (Transaction tx = graphDatabaseService.beginTx()) {
            people = convertList(iteratorToList(graphDatabaseService.findNodes(DynamicLabel.label("Person"))));
            tx.success();
        }

        if (type == PeopleType.ANY) {
            return people;
        }

        List<Person> _people = new ArrayList<>();
        if (type == PeopleType.SCANNED) {
            _people.addAll(people.stream().filter(p -> p.getUpdateDate() > LAST_UPDATE_DATE).collect(Collectors.toList()));
        }
        if (type == PeopleType.NOT_SCANNED) {
            _people.addAll(people.stream().filter(p -> p.getUpdateDate() < LAST_UPDATE_DATE).collect(Collectors.toList()));
        }

        return _people;
    }

    public void clearDatabase() {
        try (Transaction tx = graphDatabaseService.beginTx()) {
            graphDatabaseService.execute("MATCH (n) DETACH DELETE n"); //todo change to java instead of cypher
            tx.success();
        }
    }

    public List<Person> getFriendsList(String facebookId) {
        Node personNode;

        List<Person> friends;
        try (Transaction tx = graphDatabaseService.beginTx()) {
            //find person by facebookId in database
            personNode = graphDatabaseService.findNode(DynamicLabel.label("Person"), "facebookId", facebookId);

            List<Relationship> rels = iteratorToList(personNode.getRelationships(Relations.FRIEND_TO).iterator());

            List<Node> nodes = new ArrayList<>();
            for (Relationship r : rels) {
                Node node = r.getOtherNode(personNode);
                //Basically a way around of using bidirectional relationships
                if (!nodes.contains(node)) {
                    nodes.add(node);
                }
            }

            friends = convertList(nodes);
            tx.success();
        }

        return friends;
    }

    public List<List<Person>> getPaths(String facebookIdStart, String facebookIdEnd) {
        List<List<Person>> listOfPeoplePaths = new ArrayList<>();

        try(Transaction tx = graphDatabaseService.beginTx()) {
            Node startNode = graphDatabaseService.findNode(DynamicLabel.label("Person"), "facebookId", facebookIdStart);
            Node endNode = graphDatabaseService.findNode(DynamicLabel.label("Person"), "facebookId", facebookIdEnd);

            PathFinder finder = GraphAlgoFactory.shortestPath(
                    PathExpanders.forTypeAndDirection(Relations.FRIEND_TO, Direction.BOTH), MAX_DEPTH);

            Iterator<org.neo4j.graphdb.Path> iterator = finder.findAllPaths(startNode, endNode).iterator(); //todo
            List<org.neo4j.graphdb.Path> paths = iteratorToList(iterator);

            /*
            *
            * todo oooooooooooooooooooooooooooooo*/

            for (org.neo4j.graphdb.Path p : paths) {
                List<Node> nodes = iteratorToList(p.nodes().iterator());
                List<Person> peoplePath = convertList(nodes);

                listOfPeoplePaths.add(peoplePath);
            }

            tx.success();
        }


        return listOfPeoplePaths;
    }

    private List<Person> convert1temp(List<PropertyContainer> cs) {
        List<Person> nodes = new ArrayList<>();

        for (PropertyContainer c : cs) {
            Person p = new Person(c.getProperty("facebookId").toString(), c.getProperty("name").toString());
            nodes.add(p);
        }

        return nodes;
    }

    /**
     *
     *
     *
     */

    private class Neo4jServiceTools {
        //todo move service methods here
    }
}