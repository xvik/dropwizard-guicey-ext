package ru.vyarus.guicey.spa.filter;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/**
 * Response wrapper object for intercepting "sendError" calls inside assets servlet.
 *
 * @author Vyacheslav Rusakov
 * @since 02.04.2017
 */
public class ResponseWrapper extends HttpServletResponseWrapper {

    private int error;

    public ResponseWrapper(final HttpServletResponse response) {
        super((HttpServletResponse) response);
    }

    @Override
    public void sendError(final int sc, final String msg) throws IOException {
        error = sc;
    }

    @Override
    public void sendError(final int sc) throws IOException {
        error = sc;
    }

    /**
     * @return error number or 0 if error wasn't called
     */
    public int getError() {
        return error;
    }
}
