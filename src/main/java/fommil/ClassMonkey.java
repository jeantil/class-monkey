// Copyright (C) 2016 Sam Halliday
// License: http://www.gnu.org/software/classpath/license.html
package fommil;

import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.util.logging.*;

import org.objectweb.asm.*;

final class ClassMonkey implements ClassFileTransformer {
    static private final Logger log = Logger.getLogger(ClassMonkey.class.getName());

    public static void premain(String agentArgs,
                               Instrumentation inst) {

        if (!inst.isRetransformClassesSupported()) {
            log.warning("class monkey is disabled");
        } else {
            inst.addTransformer(new ClassMonkey(), true);

            try {
                // must be loaded in advance or it dumps the core
                Class.forName("fommil.URLClassPath");
            } catch (Throwable t) {
                log.warning("Failed to load fommil's URLClassPath. Monkey fail.");
            }

            try {
                inst.retransformClasses(java.net.URLClassLoader.class);
            } catch (UnmodifiableClassException e) {
                log.warning("Can't modify URLClassLoader. Monkeys are off.");
            }
        }

    }

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] origBytes) {

        if (!"java/net/URLClassLoader".equals(className)) {
            return null;
        }

        try {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

            ClassVisitor cv = new ClassMonkeyVisitor(cw, "sun/misc/URLClassPath", "fommil/URLClassPath");

            ClassReader cr = new ClassReader(origBytes);
            cr.accept(cv, ClassReader.SKIP_FRAMES);

            byte[] updatedBytes = cw.toByteArray();

            return updatedBytes;
        } catch (Throwable t) {
            log.warning("bad monkey " + t.getClass());
            t.printStackTrace();
            throw t;
        }
    }

}
