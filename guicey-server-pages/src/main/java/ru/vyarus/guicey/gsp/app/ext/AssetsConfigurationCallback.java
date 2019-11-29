package ru.vyarus.guicey.gsp.app.ext;

import ru.vyarus.dropwizard.guice.module.installer.bundle.GuiceyEnvironment;
import ru.vyarus.guicey.gsp.app.asset.AssetSources;

/**
 * Assets delayed configuration callback for server pages application extensions. Used to perform
 * configurations under run phase.
 *
 * @author Vyacheslav Rusakov
 * @since 29.11.2019
 */
@FunctionalInterface
public interface AssetsConfigurationCallback {

    /**
     * Called under run phase to perform delayed extensions configuration.
     *
     * @param environment guicey environment object
     * @param locations   object for registartion of extended locations
     */
    void configure(GuiceyEnvironment environment, AssetSources locations);
}
