package ru.vyarus.guicey.jdbi3.tx.aop;

import com.google.common.base.Throwables;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import ru.vyarus.guicey.jdbi3.tx.TransactionTemplate;

import javax.inject.Inject;

/**
 * Intercept transaction annotations usage and applies {@link TransactionTemplate} around method call.
 *
 * @since 31.08.2018
 * @author Vyacheslav Rusakov
 */
public class TransactionalInterceptor implements MethodInterceptor {

    @Inject
    private TransactionTemplate template;

    @Override
    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    public Object invoke(final MethodInvocation invocation) throws Throwable {
        return template.inTransaction(handle -> {
            try {
                return invocation.proceed();
            } catch (Throwable throwable) {
                Throwables.throwIfUnchecked(throwable);
                throw new RuntimeException(throwable);
            }
        });
    }
}
