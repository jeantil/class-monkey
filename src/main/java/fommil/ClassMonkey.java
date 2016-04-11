// Copyright (C) 2016 Sam Halliday
// Licence: http://openjdk.java.net/legal/gplv2+ce.html
package fommil;

import java.lang.instrument.Instrumentation;
import java.util.logging.Logger;

class ClassMonkey {
    static private final Logger logger = Logger.getLogger(ClassMonkey.class.getName());

    public static void premain(String agentArgs, Instrumentation inst) {
        logger.info("There are monkeys in this jar!");
    }
}
