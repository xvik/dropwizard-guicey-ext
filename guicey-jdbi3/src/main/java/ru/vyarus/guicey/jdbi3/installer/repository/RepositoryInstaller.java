package ru.vyarus.guicey.jdbi3.installer.repository;

import com.google.common.base.Preconditions;
import com.google.inject.Binder;
import com.google.inject.Binding;
import com.google.inject.Stage;
import com.google.inject.matcher.Matchers;
import ru.vyarus.dropwizard.guice.module.installer.FeatureInstaller;
import ru.vyarus.dropwizard.guice.module.installer.install.binding.BindingInstaller;
import ru.vyarus.dropwizard.guice.module.installer.util.Reporter;
import ru.vyarus.guice.ext.core.generator.DynamicClassGenerator;
import ru.vyarus.guicey.jdbi3.installer.repository.sql.SqlObjectProvider;
import ru.vyarus.guicey.jdbi3.module.NoSyntheticMatcher;
import ru.vyarus.guicey.jdbi3.tx.InTransaction;
import ru.vyarus.guicey.jdbi3.tx.TransactionTemplate;
import ru.vyarus.guicey.jdbi3.unit.UnitManager;

import javax.inject.Provider;
import javax.inject.Singleton;
import java.lang.reflect.InvocationTargetException;

/**
 * Recognize classes annotated with {@link JdbiRepository} and register them. Such classes may be then
 * injected as usual beans and used as usual daos. All daos will participate in the thread-bound transaction,
 * declared by transaction annotation, transaction template or manually with unit manager.
 * <p>
 * Dao may use any guice-related annotations because beans participate in guice aop. This is done by creating
 * special guice-managed proxy class (where guice could apply aop). These proxies delegate all method calls to
 * JDBI-managed proxies.
 *
 * @author Vyacheslav Rusakov
 * @see InTransaction default annotation
 * @see TransactionTemplate for template usage
 * @see UnitManager for low level usage without transaction
 * @since 31.08.2018
 */
public class RepositoryInstaller implements FeatureInstaller, BindingInstaller {

    private final Reporter reporter = new Reporter(RepositoryInstaller.class, "repositories = ");

    @Override
    public boolean matches(final Class<?> type) {
        return type.getAnnotation(JdbiRepository.class) != null;
    }

    @Override
    @SuppressWarnings({"unchecked", "checkstyle:Indentation"})
    public void bindExtension(final Binder binder, final Class<?> type, final boolean lazy) {
        Preconditions.checkState(!lazy, "@LazyBinding not supported");

        // jdbi on demand proxy creator: laziness required to wait for global configuration complete
        // to let proxy factory create method configs with all global configurations (mappers)
        final Provider<Object> jdbiProxy = new SqlObjectProvider(type);
        binder.requestInjection(jdbiProxy);

        // prepare non abstract implementation class (instantiated by guice)
        final Class guiceType = DynamicClassGenerator.generate(type);
        binder.bind(type).to(guiceType).in(Singleton.class);

        // interceptor registered for each dao and redirect calls to actual jdbi proxy
        // (at this point all guice interceptors are already involved)
        binder.bindInterceptor(Matchers.subclassesOf(type), NoSyntheticMatcher.instance(),
                invocation -> {
                    try {
                        return invocation.getMethod().invoke(jdbiProxy.get(), invocation.getArguments());
                    } catch (InvocationTargetException th) {
                        // avoid exception wrapping (simpler to handle outside)
                        throw th.getCause();
                    }
                });
    }

    @Override
    public <T> void checkBinding(final Binder binder, final Class<T> type, final Binding<T> manualBinding) {
        // it's impossible to bind manually abstract type in guice
    }

    @Override
    public void installBinding(final Binder binder, final Class<?> type) {
        if (binder.currentStage() != Stage.TOOL) {
            reporter.line(String.format("(%s)", type.getName()));
        }
    }

    @Override
    public void report() {
        reporter.report();
    }
}
