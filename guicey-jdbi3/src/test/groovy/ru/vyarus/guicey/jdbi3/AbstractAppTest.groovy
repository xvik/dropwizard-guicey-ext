package ru.vyarus.guicey.jdbi3

import ru.vyarus.dropwizard.guice.test.spock.UseGuiceyApp
import ru.vyarus.guicey.jdbi3.support.SampleApp

/**
 * @author Vyacheslav Rusakov
 * @since 31.08.2018
 */
@UseGuiceyApp(value = SampleApp, config = 'src/test/resources/test-config.yml')
abstract class AbstractAppTest extends AbstractTest {
}
