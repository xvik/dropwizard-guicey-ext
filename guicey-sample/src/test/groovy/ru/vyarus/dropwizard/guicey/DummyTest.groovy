package ru.vyarus.dropwizard.guicey;

/**
 * Dummy test.
 *
 * @author Vyacheslav Rusakov
 * @since 30.09.2016
 */
class DummyTest extends AbstractTest {

    def "Check something important"() {

        when: "do something"
        Integer checkAssignment = 1
        then: "check result"
        checkAssignment == 1
    }
}
