// Copyright 1999 Oracle and/or its affiliates
// Copyright 2016 Sam Halliday
// License: http://openjdk.java.net/legal/gplv2+ce.html
package com.sun.net.httpserver;

import java.net.*;
import java.util.*;
import java.text.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import com.sun.net.httpserver.*;
import javax.security.auth.*;
import javax.security.auth.callback.*;
import javax.security.auth.login.*;

class LogFilter extends Filter {

    PrintStream ps;
    DateFormat df;

    LogFilter (File file) throws IOException {
        ps = new PrintStream (new FileOutputStream (file));
        df = DateFormat.getDateTimeInstance();
    }

    /**
     * The filter's implementation, which is invoked by the serve r
     */
    public void doFilter (HttpExchange t, Filter.Chain chain) throws IOException
    {
        chain.doFilter (t);
        HttpContext context = t.getHttpContext();
        Headers rmap = t.getRequestHeaders();
        String s = df.format (new Date());
        s = s +" " + t.getRequestMethod() + " " + t.getRequestURI() + " ";
        s = s +" " + t.getResponseCode () +" " + t.getRemoteAddress();
        ps.println (s);
    }

    public void init (HttpContext ctx) {}

    public String description () {
        return "Request logger";
    }

    public void destroy (HttpContext c){}
}
