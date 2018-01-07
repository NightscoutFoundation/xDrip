package com.eveningoutpost.dexdrip.webservices;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by jamorham on 06/01/2018.
 * <p>
 * Base class for web services
 */

public abstract class BaseWebService {

    private static String TAG = "BaseWebService";

    public static BaseWebService getInstance() {
        return null; // stub
    }

    public abstract WebResponse request(String query);

    // return a list splitting on path delimiter of /
    static List<String> getUrlComponents(String query) {
        final StringTokenizer tokenizer = new StringTokenizer(query, "/");
        final List<String> tokenizer_list = new ArrayList<>();
        while (tokenizer.hasMoreTokens()) {
            tokenizer_list.add(tokenizer.nextToken());
        }
        return tokenizer_list;
    }

    // remove first path so /abc/xyz/123 becomes xyz/123
    static String stripFirstComponent(String query) {
        try {
            int start = query.indexOf('/') + 1;
            return query.substring(start, query.length());
        } catch (Exception e) {
            return "";
        }
    }

}
