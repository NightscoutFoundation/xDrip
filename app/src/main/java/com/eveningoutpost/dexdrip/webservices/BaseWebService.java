package com.eveningoutpost.dexdrip.webservices;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Created by jamorham on 06/01/2018.
 * <p>
 * Base class for web services
 */

public abstract class BaseWebService {

    public abstract WebResponse request(String query);

    public WebResponse request(final String query, final InetAddress source) {
        try {
            return request(query);
        } catch (Exception ex) {
            return webError("Exception in "+getClass()+": " +ExceptionUtils.getStackTrace(ex), 500);
        }
    }

    // web error helpers
    WebResponse webError(String msg, int resultCode) {
        return new WebResponse(msg + "\n", resultCode, "text/plain");
    }

    // default error type is 400, bad request
    WebResponse webError(String msg) {
        return webError(msg, 400);
    }

    // success responses
    WebResponse webOk(String msg) {
        return webError(msg, 200);
    }


    // return a list splitting on path delimiter of /
    static List<String> getUrlComponents(String query) {
        return getUrlComponents(query, "/");
    }

    // return a list splitting on delimiter
    static List<String> getUrlComponents(String query, String delimiter) {
        final StringTokenizer tokenizer = new StringTokenizer(query, delimiter);
        final List<String> tokenizer_list = new ArrayList<>();
        while (tokenizer.hasMoreTokens()) {
            tokenizer_list.add(tokenizer.nextToken());
        }
        return tokenizer_list;
    }

    static Map<String, String> getQueryParameters(String query) {
        final Map<String, String> cgi = new HashMap<>();
        try {
            query = stripFirstComponent(query, '?');
            final List<String> pairs = getUrlComponents(query, "&");
            for (String pair : pairs) {
                final List<String> value = getUrlComponents(pair, "=");
                if (value.size() > 1) {
                    cgi.put(value.get(0), value.get(1)); // abc = def
                }
            }
        } catch (Exception e) {
            // parse error
        }
        return cgi;
    }


    // remove first path so /abc/xyz/123 becomes xyz/123
    static String stripFirstComponent(String query) {
        return stripFirstComponent(query, '/');
    }

    // remove first item preceeding the delimiter
    static String stripFirstComponent(String query, char delimiter) {
        try {
            int start = query.indexOf(delimiter) + 1;
            return query.substring(start, query.length());
        } catch (Exception e) {
            return "";
        }
    }

    static String urlDecode(final String msg) {
        try {
            return URLDecoder.decode(msg, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null; // TODO best response?
        }
    }


}
