package HandshakeServer.files;

import HandshakeServer.Neo4jDb.Person;
import HandshakeServer.Neo4jDb.Neo4jService;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.fusesource.jansi.AnsiConsole.out;

/**
 * Created by hleb on 4/18/16.
 *
 * todo make static?
 */

@Component
public class FileService {

    /******************** Writing ids to be scanned to file ********************/

    @Autowired private Neo4jService neo4jService;
    @Autowired private ApplicationContext applicationContext;
    @Autowired private Logger log;

    public int writeIds(Path path, int peopleCount) {
        log.info("Getting not scanned people from database");
        List<Person> peopleToScan = neo4jService.fetchPeopleToScan(peopleCount); //speed up query
        log.info("Writing to " + path.toAbsolutePath());
        try {
            Files.write(path, getStringIds(peopleToScan), Charset.forName("UTF-8"));
        } catch (IOException ex) {ex.printStackTrace(); return 0;}
        log.info("Done");

        return peopleToScan.size();
   }

    private List<String> getStringIds(List<Person> people) {
        List<String> list = new ArrayList<>();

        for (int i = 0; i < people.size(); i++) {
            list.add(people.get(i).getFacebookId());
        }
        return list;
    }

    /******************** Reading scanned people and saving data to db ********************/

    public int readToDb(Path path) {
        try {
            log.info("Reading from " + path.toAbsolutePath());
            List<Person> scannedPeople = readFiles(path);

            if (scannedPeople.size() == 0) {
                return 0;
            }

            log.info("Writing info to database");
            neo4jService.save(scannedPeople);
            return scannedPeople.size();
        } catch (IOException ex) {
            ex.printStackTrace();
            return 0;
        }
    }

    private List<Person> readFiles(Path path) throws IOException {
        List<Person> scannedPeople = new ArrayList<>();
        File peopleFolder = new File(path.toUri());

        if (!peopleFolder.exists()) {
            System.out.println("Folder does not exist");
            return scannedPeople; //empty
        }

        for (File f : peopleFolder.listFiles()) {
            scannedPeople.add(getPerson(f));
        }

        return scannedPeople;
    }

    private Person getPerson(File file) throws IOException { //returns one person which corresponds to file
        String facebookId;
        String name;
        List<Person> friends = new ArrayList<>();
        Person friend;

        List<String> entries = readEntries(file); //defining friends list

        for (int i = 0; i < entries.size(); i++) {
            String entry = entries.get(i); //facebookId:name
            facebookId = entry.substring(0, entry.indexOf(':'));
            name = entry.substring(entry.indexOf(':')+1);

            friend = applicationContext.getBean(Person.class, facebookId, name);
            friends.add(friend);
        }

        String filename = FilenameUtils.getBaseName(file.toString()); //defining person's properties
        String personFacebookId = filename.substring(0, filename.indexOf(':'));
        long updateDate = Long.valueOf(filename.substring(filename.indexOf(':')+1));

        Person person = applicationContext.getBean(Person.class, personFacebookId, null);
        person.setUpdateDate(updateDate);
        person.setFriends(friends);
        return person;
    }

    private List<String> readEntries(File file) throws IOException {
        List<String> entries = new ArrayList<>();

        Stream<String> s = Files.lines(file.toPath());
        s.forEach(entries::add);
        s.close();

        return entries;
    }
}
