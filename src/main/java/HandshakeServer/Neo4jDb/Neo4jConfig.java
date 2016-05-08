package HandshakeServer.Neo4jDb;

import HandshakeServer.files.FileService;
import org.apache.log4j.Logger;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.UniqueFactory;
import org.springframework.context.annotation.*;

import java.io.File;
import java.util.Map;

/**
 * Created by hleb on 3/14/16.
 *
 *
 *
 * Works with Neo4j 2.3.3
 *
 *
 */

@Configuration
@ComponentScan("HandshakeServer")
public class Neo4jConfig {

    private final String DB_PATH = "/home/hleb/apps/NEO4J_HOME/neo4j-community-2.3.3/data/graph.db"; //todo move to properties

    @Bean
    Logger getLogger() {
        return Logger.getRootLogger();
    }

    @Bean
    public Neo4jService getNeo4jService() {
        return new Neo4jService();
    }

    @Bean(destroyMethod = "shutdown")
    public GraphDatabaseService getGraphDatabaseService() {
        return new GraphDatabaseFactory().newEmbeddedDatabase(new File(DB_PATH));
    }

    @Bean
    @Scope(value = "prototype")
    Person getLogicPerson(String facebookId, String name) {
        return new Person(facebookId, name);
    }

    @Bean(name = "factory")
    public UniqueFactory.UniqueNodeFactory factory(){
        GraphDatabaseService dbs = getGraphDatabaseService();

        try (Transaction tx = dbs.beginTx()) {
            UniqueFactory.UniqueNodeFactory factory = new UniqueFactory.UniqueNodeFactory(dbs, "people") {
                @Override
                protected void initialize(Node node, Map<String, Object> props) {
                    node.addLabel(DynamicLabel.label("Person"));
                    node.setProperty("facebookId", props.get("facebookId"));
                }
            };
            tx.success();
            return factory;
        }
    }

    @Bean
    FileService fileService() {
        return new FileService();
    }
}
