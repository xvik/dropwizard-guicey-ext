package ru.vyarus.guicey.jdbi3.tx;


import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.lang3.tuple.Pair;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.transaction.TransactionException;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import ru.vyarus.guicey.jdbi3.unit.UnitManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Transaction template used to both declare unit of work and start transaction.
 * If called inside of transaction then provided action will be simply executed as transaction is already managed
 * somewhere outside. In case of exception, it's propagated and transaction rolled back.
 * <p>
 * Usage:
 * <pre><code>
 *    {@literal @}Inject TransactionTemplate template;
 *     ...
 *     template.inTransaction(() -&gt; doSoemStaff())
 * </code></pre>
 *
 * @author Vyacheslav Rusakov
 * @since 31.08.2018
 */
@Singleton
public class TransactionTemplate {

    private final UnitManager manager;

    private final LoadingCache<Handle, Pair<TransactionIsolationLevel, Long>> isolationLevelCache;

    @Inject
    public TransactionTemplate(final UnitManager manager) {
        this.manager = manager;
        CacheLoader<Handle, Pair<TransactionIsolationLevel, Long>> loader = new CacheLoader<>() {
            @Override
            public Pair<TransactionIsolationLevel, Long> load(final Handle key) {
                return Pair.of(key.getTransactionIsolationLevel(), System.currentTimeMillis());
            }
        };

        // cache 8 handles at a time for a maximum of 3 minutes
        this.isolationLevelCache = CacheBuilder.newBuilder().maximumSize(8).expireAfterWrite(Duration.ofMinutes(3)).build(loader);
    }

    /**
     * Shortcut for {@link #inTransaction(TxConfig, TxAction)} for calling action with default transaction config.
     *
     * @param action action to execute
     * @param <T>    return type
     * @return action result
     */
    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    public <T> T inTransaction(final TxAction<T> action) {
        return inTransaction(new TxConfig(), action);
    }

    /**
     * Wraps provided action with unit of work and transaction. If called under already started transaction
     * then action will be called directly.
     * <p>
     * NOTE: If unit of work was started manually (using {@link UnitManager}, but without transaction started,
     * then action will be simply executed without starting transaction. This was done for rare situations
     * when logic must be performed without transaction and transaction annotation will simply indicate unit of work.
     *
     * @param config transaction config
     * @param action action to execute
     * @param <T>    return type
     * @return action result
     */
    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    public <T> T inTransaction(final TxConfig config, final TxAction<T> action) {
        if (manager.isUnitStarted()) {
            // already started
            try {
                return inCurrentTransaction(config, action);
            } catch (Throwable th) {
                Throwables.throwIfUnchecked(th);
                throw new RuntimeException(th);
            }
        } else {
            manager.beginUnit();
            try {
                return inNewTransaction(config, action);
            } finally {
                manager.endUnit();
            }
        }
    }


    private <T> T inCurrentTransaction(final TxConfig config, final TxAction<T> action) throws Exception {
        // mostly copies org.jdbi.v3.sqlobject.transaction.internal.TransactionDecorator logic
        final Handle h = manager.get();

        TransactionIsolationLevel currentLevel;

        // if isolation levels are configured to be cached, then do so and update the cache appropriately
        if (config.getMsIsolationLevelCacheTime() > 0) {
            // this bit of logic is a workaround to the fact that we cannot access the requested transaction isolation
            // level cache time at the time we construct the TransactionTemplate and so must expire values from our cache
            // manually.

            // first use the cache to get or insert the current connection's isolation level
            final Pair<TransactionIsolationLevel, Long> isolationLevelDetails = isolationLevelCache.get(h);
            final Long cachedTimeLevel = isolationLevelDetails.getRight();

            // if we have a cache length time, and it has been more than that many ms since the isolation level was polled
            // from the connection, then refresh the cache by querying the connection
            if (config.getMsIsolationLevelCacheTime() > 0 &&
                    ((System.currentTimeMillis() - cachedTimeLevel) > config.getMsIsolationLevelCacheTime())) {
                isolationLevelCache.refresh(h);
            }

            // pull from the cache which is either the previously cached value or the recently refreshed one
            currentLevel = isolationLevelCache.get(h).getLeft();
        } else {
            // default behavior: always get the transaction isolation level from the underlying connection
            currentLevel = h.getTransactionIsolationLevel();
        }

        if (config.isLevelSet() && currentLevel != config.getLevel()) {
            throw new TransactionException("Tried to execute nested @Transaction(" + config.getLevel() + "), " + "but already running in a transaction with isolation level " + currentLevel + ".");
        }
        if (h.isReadOnly() && !config.isReadOnly()) {
            throw new TransactionException("Tried to execute a nested @Transaction(readOnly=false) " + "inside a readOnly transaction");
        }
        return action.execute(h);
    }

    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    private <T> T inNewTransaction(final TxConfig config, final TxAction<T> action) {
        final Handle h = manager.get();
        h.setReadOnly(config.isReadOnly());
        final HandleCallback<T, RuntimeException> callback = handle -> {
            try {
                return action.execute(handle);
            } catch (Exception e) {
                Throwables.throwIfUnchecked(e);
                throw new RuntimeException(e);
            }
        };
        return config.isLevelSet() ? h.inTransaction(config.getLevel(), callback) : h.inTransaction(callback);
    }
}
