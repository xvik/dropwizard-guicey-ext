package ru.vyarus.guicey.gsp

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
    HTTPBuilder mainHttp = new HTTPBuilder('http://localhost:8080/')
    HTTPBuilder adminHttp = new HTTPBuilder('http://localhost:8081/')

    // shortcut to return body

    String get(String url) {
        call(mainHttp, url, false)
    }

    String getHtml(String url) {
        call(mainHttp, url, true)
    }

    String adminGet(String url) {
        call(adminHttp, url, false)
    }

    String adminGetHtml(String url) {
        call(adminHttp, url, true)
    }

    private String call(HTTPBuilder http, String path, boolean html) {
        try {
            // text if not html to avoid HTTPBuilder response parsing attempt (possible with wildcard)
            return http.get(path: path, contentType: html ? ContentType.HTML : ContentType.ANY) { resp, reader ->
                html? reader : reader.text
            }
        } catch (HttpResponseException ex) {
            if (ex.statusCode == 404) {
                throw new FileNotFoundException()
            }
            throw ex
        }
    }
}
