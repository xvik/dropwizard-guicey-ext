package ru.vyarus.guicey.gsp.util

import ru.vyarus.guicey.gsp.app.util.PathUtils
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 29.11.2019
 */
class PathUtilsTest extends Specification {

    def "Check path normalization"() {

        expect:
        PathUtils.normalizePath(base, path) == res

        where:
        base         | path          | res
        '/'          | 'sample.txt'  | '/sample.txt'
        ''           | 'sample.txt'  | 'sample.txt'
        '/foo/'      | '/sample.txt' | '/foo/sample.txt'
        '  /foo/  '  | '/sample.txt' | '/foo/sample.txt'
        '/foo\\bar/' | '/sample.txt' | '/foo/bar/sample.txt'
    }

    def "Check path cleanup"() {

        expect:
        PathUtils.cleanUpPath(path) == res

        where:
        path           | res
        '/some/'       | '/some/'
        '//some//foo'  | '/some/foo'
        'some / foo'   | 'some/foo'
        'some / / foo' | 'some/foo'
        'some\\foo'    | 'some/foo'
    }

    def "Check prefix slash"() {

        expect:
        PathUtils.prefixSlash(path) == res

        where:
        path    | res
        ''      | '/'
        '/'     | '/'
        'foo'   | '/foo'
        '/foo/' | '/foo/'
    }

    def "Check end slash"() {

        expect:
        PathUtils.endSlash(path) == res

        where:
        path    | res
        ''      | ''
        '/'     | '/'
        'foo'   | 'foo/'
        '/foo/' | '/foo/'
    }

    def "Check trim stars"() {

        expect:
        PathUtils.trimStars(path) == res

        where:
        path     | res
        ''       | ''
        '/'      | '/'
        '/*'     | '/'
        '*/'     | '/'
        '/foo/*' | '/foo/'
        '*/*/*'  | '/*/'
    }

    def "Check trim slashes"() {

        expect:
        PathUtils.trimSlashes(path) == res

        where:
        path       | res
        ''         | ''
        '/'        | ''
        '/foo/'    | 'foo'
        '/foo/bar' | 'foo/bar'
        'foo/bar/' | 'foo/bar'
    }

    def "Check get path"() {
        expect:
        PathUtils.getPath(cls) == res

        where:
        cls       | res
        Integer   | 'java/lang'
        PathUtils | 'ru/vyarus/guicey/gsp/app/util'
    }

    def "Check relative path normalization"() {
        expect:
        PathUtils.normalizeRelativePath(path) == res

        where:
        path         | res
        '/'          | ''
        ''           | ''
        '/foo/'      | 'foo/'
        '  /foo/  '  | 'foo/'
        '/foo\\bar/' | 'foo/bar/'
        '/foo/bar'   | 'foo/bar/'
    }

    def "Check classpath path normalization"() {
        expect:
        PathUtils.normalizeClasspathPath(path) == res

        where:
        path          | res
        '/'           | ''
        ''            | ''
        '/foo/'       | 'foo/'
        '  /foo/  '   | 'foo/'
        '/foo\\bar/'  | 'foo/bar/'
        '/foo/bar'    | 'foo/bar/'
        'foo.bar'     | 'foo/bar/'
        'foo . bar'   | 'foo/bar/'
        '  foo.bar  ' | 'foo/bar/'
    }
}
