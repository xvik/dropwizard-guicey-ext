package ru.vyarus.guicey.spa

import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseException
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 29.11.2019
 */
abstract class AbstractTest extends Specification {

    // default builder for text/html type (user call simulation)
    HTTPBuilder mainHttp = new HTTPBuilder('http://localhost:8080/', ContentType.HTML)
    HTTPBuilder adminHttp = new HTTPBuilder('http://localhost:8081/', ContentType.HTML)

    // shortcut to return body
    String get(String url) {
        call(mainHttp, url)
    }

    String adminGet(String url) {
        call(adminHttp, url)
    }

    private String call(HTTPBuilder http, String path) {
        try {
            return http.get(path: path) { resp, reader ->
                reader
            }
        } catch (HttpResponseException ex) {
            if (ex.statusCode == 404) {
                throw new FileNotFoundException()
            }
            throw ex
        }
    }
}
