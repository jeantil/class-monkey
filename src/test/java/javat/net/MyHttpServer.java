// Copyright 1999 Oracle and/or its affiliates
// Copyright 2016 Sam Halliday
// License: http://openjdk.java.net/legal/gplv2+ce.html
package javat.net;

import java.io.*;
import java.net.*;

/*
 * "Simple" http server to service http requests. Auto shutdown if
 * "idle" (no requests) for 10 seconds. Forks worker thread to service
 * persistent connections. Work threads shutdown if "idle" for 5
 * seconds.
 */
class MyHttpServer implements Runnable {

    private static MyHttpServer svr = null;
    private static Counters cnts = null;
    private static ServerSocket ss;

    private static Object counterLock = new Object();
    private static int getCount = 0;
    private static int headCount = 0;

    class Worker extends Thread {
        Socket s;
        Worker(Socket s) {
            this.s = s;
        }

        public void run() {
            InputStream in = null;
            try {
                in = s.getInputStream();
                for (;;) {

                    // read entire request from client
                    byte b[] = new byte[1024];
                    int n, total=0;

                    // max 5 seconds to wait for new request
                    s.setSoTimeout(5000);
                    try {
                        do {
                            n = in.read(b, total, b.length-total);
                            // max 0.5 seconds between each segment
                            // of request.
                            s.setSoTimeout(500);
                            if (n > 0) total += n;
                        } while (n > 0);
                    } catch (SocketTimeoutException e) { }

                    if (total == 0) {
                        s.close();
                        return;
                    }

                    boolean getRequest = false;
                    if (b[0] == 'G' && b[1] == 'E' && b[2] == 'T')
                        getRequest = true;

                    synchronized (counterLock) {
                        if (getRequest)
                            getCount++;
                        else
                            headCount++;
                    }

                    // response to client
                    PrintStream out = new PrintStream(
                                                      new BufferedOutputStream(
                                                                               s.getOutputStream() ));
                    out.print("HTTP/1.1 200 OK\r\n");

                    out.print("Content-Length: 75000\r\n");
                    out.print("\r\n");
                    if (getRequest) {
                        for (int i=0; i<75*1000; i++) {
                            out.write( (byte)'.' );
                        }
                    }
                    out.flush();

                } // for

            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (in != null)
                    try {
                        in.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
            }
        }
    }

    MyHttpServer() throws Exception {
        ss = new ServerSocket(0);
    }

    public void run() {
        try {
            // shutdown if no request in 10 seconds.
            ss.setSoTimeout(10000);
            for (;;) {
                Socket s = ss.accept();
                (new Worker(s)).start();
            }
        } catch (Exception e) {
        }
    }

    public static MyHttpServer create() throws Exception {
        if (svr != null)
            return svr;
        cnts = new Counters();
        svr = new MyHttpServer();
        (new Thread(svr)).start();
        return svr;
    }

    public static void shutdown() throws Exception {
        if (svr != null) {
            ss.close();
            svr = null;
        }
    }

    public int port() {
        return ss.getLocalPort();
    }

    public static class Counters {
        public void reset() {
            synchronized (counterLock) {
                getCount = 0;
                headCount = 0;
            }
        }

        public int getCount() {
            synchronized (counterLock) {
                return getCount;
            }
        }

        public int headCount() {
            synchronized (counterLock) {
                return headCount;
            }
        }

        public String toString() {
            synchronized (counterLock) {
                return "GET count: " + getCount + "; " +
                    "HEAD count: " + headCount;
            }
        }
    }

    public Counters counters() {
        return cnts;
    }

}
