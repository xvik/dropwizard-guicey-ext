package ru.vyarus.guicey.jdbi.dbi;

import io.dropwizard.Configuration;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.setup.Environment;
import org.skife.jdbi.v2.DBI;

/**
 * Simple DBI configurer, requiring just database configuration.
 *
 * @author Vyacheslav Rusakov
 * @since 05.12.2016
 */
public class SimpleDbiProvider implements ConfigAwareProvider<DBI> {

    private final ConfigAwareProvider<PooledDataSourceFactory> database;

    public SimpleDbiProvider(final ConfigAwareProvider<PooledDataSourceFactory> database) {
        this.database = database;
    }

    @Override
    public DBI get(final Environment environment, final Configuration configuration) {
        return new DBIFactory().build(environment, database.get(environment, configuration), "db");
    }
}
