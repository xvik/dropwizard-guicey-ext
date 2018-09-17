package ru.vyarus.guicey.jdbi3.installer;

import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import org.jdbi.v3.core.mapper.RowMapper;
import ru.vyarus.dropwizard.guice.module.installer.FeatureInstaller;
import ru.vyarus.dropwizard.guice.module.installer.install.binding.BindingInstaller;
import ru.vyarus.dropwizard.guice.module.installer.util.FeatureUtils;
import ru.vyarus.dropwizard.guice.module.installer.util.Reporter;
import ru.vyarus.guicey.jdbi3.module.MapperBinder;
import ru.vyarus.java.generics.resolver.GenericsResolver;

import javax.inject.Singleton;

/**
 * Recognize classes implementing JDBI's {@link org.jdbi.v3.core.mapper.RowMapper} and register them.
 * Register mappers as singletons. Reports all installed mappers to console.
 * <p>
 * Mappers are normal guice beans and so may use constructor injection, aop etc.
 *
 * @author Vyacheslav Rusakov
 * @see MapperBinder for actual installation
 * @see <a href="http://jdbi.org/#_row_mappers">row mappers doc</a>
 * @since 31.08.2018
 */
public class MapperInstaller implements FeatureInstaller<RowMapper>,
        BindingInstaller {

    private final Reporter reporter = new Reporter(MapperInstaller.class, "jdbi row mappers = ");

    @Override
    public boolean matches(final Class<?> type) {
        return FeatureUtils.is(type, RowMapper.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void install(final Binder binder, final Class<? extends T> type, final boolean lazy) {
        binder.bind(type).in(Singleton.class);
        // just combine mappers in set and special bean, installed by module will bind it to dbi
        Multibinder.newSetBinder(binder, RowMapper.class).addBinding()
                .to((Class<? extends RowMapper>) type);

        final String target = GenericsResolver.resolve(type).type(RowMapper.class).genericAsString(0);
        reporter.line("%-20s (%s)", target, type.getName());
    }

    @Override
    public void report() {
        reporter.report();
    }
}
