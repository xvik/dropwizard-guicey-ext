package org.jdbi.v3.core;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.ExtensionMethod;
import org.jdbi.v3.core.extension.HandleSupplier;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.concurrent.Callable;

/**
 * Bridge have to lie in jdbi package in order have access to internal methods. Implementation is the same
 * as in {@link ConstantHandleSupplier}, except handler and config are obtained dynamically.
 *
 * @author Vyacheslav Rusakov
 * @since 31.08.2018
 */
@Singleton
public class TransactionalHandleSupplier implements HandleSupplier {

    private final Jdbi jdbi;
    private final Provider<Handle> handleProvider;

    @Inject
    public TransactionalHandleSupplier(final Jdbi jdbi, final Provider<Handle> handleProvider) {
        this.jdbi = jdbi;
        this.handleProvider = handleProvider;
    }

    @Override
    public Handle getHandle() {
        return handleProvider.get();
    }

    @Override
    public <V> V invokeInContext(final ExtensionMethod extensionMethod,
                                 final ConfigRegistry config,
                                 final Callable<V> task)
            throws Exception {
        // implementation copied from ConstantHandleSupplier
        final Handle handle = getHandle();
        final ExtensionMethod oldExtensionMethod = handle.getExtensionMethod();
        try {
            handle.setExtensionMethod(extensionMethod);

            final ConfigRegistry oldConfig = handle.getConfig();
            try {
                handle.setConfig(config);
                return task.call();
            } finally {
                handle.setConfig(oldConfig);
            }
        } finally {
            handle.setExtensionMethod(oldExtensionMethod);
        }
    }

    @Override
    public ConfigRegistry getConfig() {
        return jdbi.getConfig();
    }
}
