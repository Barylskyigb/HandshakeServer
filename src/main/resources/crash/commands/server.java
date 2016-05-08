package commands;


import HandshakeServer.Neo4jDb.Person;
import HandshakeServer.Neo4jDb.Neo4jService;
import HandshakeServer.Neo4jDb.PeopleType;
import HandshakeServer.files.FileService;
import org.crsh.cli.*;
import org.crsh.command.BaseCommand;
import org.crsh.command.InvocationContext;
import org.springframework.beans.factory.BeanFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Usage("server commands")
public class server extends BaseCommand {

    @Command
    @Usage("populate server with 1 person. Use with empty database") // TODO: 5/5/16 check if db is empty
    public void popul(InvocationContext context) {
        BeanFactory beanFactory = (BeanFactory) context.getAttributes().get("spring.beanfactory");
        Neo4jService service = beanFactory.getBean(Neo4jService.class);

        service.createStartNode();
        out.println("First node created");
    }



    @Command
    @Usage("write scraped info to database (from people/ folder by default)")
    public void writetodb(
            InvocationContext context,
            @Option(names={"f", "folder"}) @Usage("folder to write from") String folder) {

        BeanFactory beanFactory = (BeanFactory) context.getAttributes().get("spring.beanfactory");
        FileService fileService = beanFactory.getBean(FileService.class);

        if (folder == null) {
            folder = "people";
        }
        out.println("Using [" + Paths.get(folder).toAbsolutePath() + "/] folder");

        Path path = Paths.get(folder);
        int written = fileService.readToDb(path);
        out.println("Wrote " + written + " people to database");
    }

    @Command
    @Usage("write ids list to scan (to ids.txt by default)")
    public void writetotxt(
            InvocationContext context,
            @Option(names={"f", "file"}) @Usage("file to write to") String file,
            @Option(names={"a", "amount"}) @Usage("amount of ids to scan") Integer amount) {

        BeanFactory beanFactory = (BeanFactory) context.getAttributes().get("spring.beanfactory");
        FileService fileService = beanFactory.getBean(FileService.class);

        if (file == null) {
            file = "ids.txt";
        }
        out.println("File is [" + Paths.get(file).toAbsolutePath() + "]");
        if (amount == null) {
            amount = 10000;
        }
        out.println("Amount of ids is " + amount);

        Path path = Paths.get(file); //works in IDE
        double start = System.currentTimeMillis();
        int written = fileService.writeIds(path, amount);
        double end = System.currentTimeMillis();

        out.println("Wrote " + written + " ids to [" + file
                + "] in " + (end-start)/1000 + " seconds");
    }

    @Command
    @Usage("returns amount of nodes in database")
    public String nodes(InvocationContext context,
                        @Option(names = {"s", "scanned"})
                        @Usage("returns amount of scanned people")
                        Boolean scanned,
                        @Option(names = {"u", "unscanned"})
                        @Usage("returns amount of unscanned people")
                        Boolean unscanned
    ) {
        BeanFactory beanFactory = (BeanFactory) context.getAttributes().get("spring.beanfactory");
        Neo4jService service = beanFactory.getBean(Neo4jService.class);

        Integer nodesCount;
        if (scanned != null) {
            if (scanned == true) {
                nodesCount = service.getPeople(PeopleType.SCANNED).size();
                return "Database has " + nodesCount + " scanned people";
            }
        }
        if (unscanned != null) {
            if (unscanned == true) {
                nodesCount = service.getPeople(PeopleType.NOT_SCANNED).size();
                return "Database has " + nodesCount + " unscanned people";
            }
        }

        nodesCount = service.getPeople(PeopleType.ANY).size();
        return "Database has " + nodesCount + " people";
    }

    @Command
    @Usage("Deletes all nodes in database")
    public void cleardb(InvocationContext context) {
        try {
            String answer = context.readLine("This will clear all nodes in database. Continue?" + "\n" + "yes/no", true);

            if (answer != null) {
                if (answer.equals("yes")) {
                    BeanFactory beanFactory = (BeanFactory) context.getAttributes().get("spring.beanfactory");
                    Neo4jService service = beanFactory.getBean(Neo4jService.class);
                    service.clearDatabase();
                    out.println("Ok");
                }
            }
        } catch (IOException ex) {ex.printStackTrace();
        } catch (InterruptedException ex1) {ex1.printStackTrace();}
    }

    @Command
    @Usage("Looks for person by facebookId and returns their friends")
    public List<String> getfriends(InvocationContext context,
                           @Required @Argument String facebookId
    ) {
        if (facebookId == null) return null;
        BeanFactory beanFactory = (BeanFactory) context.getAttributes().get("spring.beanfactory");
        Neo4jService service = beanFactory.getBean(Neo4jService.class);

        List<Person> friends = service.getFriendsList(facebookId);
        List<String> names = new ArrayList<>();
        for (Person p : friends) {
            names.add(p.getName());
        }

        out.println(names.size() + "\n");

        return names;
    }
}