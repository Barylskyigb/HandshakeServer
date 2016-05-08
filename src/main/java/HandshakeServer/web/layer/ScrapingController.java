package HandshakeServer.web.layer;

import HandshakeServer.Neo4jDb.Person;
import HandshakeServer.Neo4jDb.Neo4jService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by hleb on 4/9/16.
 * 
 */
public class ScrapingController {

    private final int peopleToFetchCount = 3;

    @Autowired private Neo4jService neo4jService;
    @Autowired Logger logger;

    public void start() {
        List<Person> notScannedPeople;
        List<Person> scannedPeople = new ArrayList<>();
        notScannedPeople = populateNotScannedPeopleList();
        //Person mainPerson = new Person("100007631628945", "Hleb Be");
        //Person mainPerson = new Person("jakub.hamiga.5");

        int iterations = 0;
        Person scannedPerson;
        Person previousPerson;

        double start = System.currentTimeMillis();

        while (notScannedPeople.size() > 0) {
            scannedPerson = notScannedPeople.get(0);
            scannedPerson.getFriends(); //make sure scannedPerson will have list of their friends

            previousPerson = scannedPerson;
            notScannedPeople.remove(scannedPerson);
            scannedPeople.add(scannedPerson); //now scannedPerson has a list of friends inside
            scannedPerson.setUpdateDate(System.currentTimeMillis()/1000);

            iterations++;

            if ((System.currentTimeMillis() - start)/1000 > 30) {
                File log = new File("15minOut.txt");

                try{
                    FileWriter fileWriter = new FileWriter(log, true);

                    BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                    bufferedWriter.write(new Date().toString() + " - current id: " + scannedPerson.getFacebookId() + ", friends: " + scannedPerson.getFriends().size() + "\n");
                    bufferedWriter.close();

                    System.out.println("Done");
                } catch(IOException e) {
                    System.out.println("Could not log!");
                }

                start = System.currentTimeMillis();
            }

            logger.info(scannedPeople.size() + " are scanned, "
                    + notScannedPeople.size() + " are not scanned, "
                    + (System.currentTimeMillis() - start)/1000 + " seconds has passed, "
                    + "previous person is " + previousPerson.getFacebookId()
                    + ", iteration number is " + iterations
                    + ", " + scannedPerson.getFriends().size() + " friends are fetched");

            if (notScannedPeople.size() == 0) {
                neo4jService.save(scannedPeople);
                scannedPeople.clear();
                notScannedPeople = populateNotScannedPeopleList();

                iterations = 0;
                logger.info("***************************** info is saved in database");
            }
        }
    }

    private List<Person> populateNotScannedPeopleList() {
        return (neo4jService.fetchPeopleToScan(peopleToFetchCount));
    }
}
