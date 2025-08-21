package innowise.payments_service.config;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiquibaseConfig {
    @Value("${spring.liquibase.url}")
    private String liquibaseUrl;

    @Value("${spring.data.mongodb.username}")
    private String username;

    @Value("${spring.data.mongodb.password}")
    private String password;

    @Bean
    public Liquibase liquibase() throws LiquibaseException {
        Database database = DatabaseFactory.getInstance()
                .openDatabase(
                        liquibaseUrl,
                        username,
                        password,
                        null,
                        new ClassLoaderResourceAccessor()
                );

        Liquibase liquibase = new Liquibase(
                "db/changelog/db.changelog-master.yaml",
                new ClassLoaderResourceAccessor(),
                database
        );

        liquibase.update("");
        return liquibase;
    }
}
