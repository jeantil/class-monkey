// Copyright (C) 2016 Sam Halliday
// Licence: http://openjdk.java.net/legal/gplv2+ce.html
package fommil;

import org.objectweb.asm.*;

/**
 * When the class tries to create a `from`, instead create a `to`.
 *
 * Don't forget to run on inner classes, if relevant.
 *
 * ClassRemapper looks like it might do this, but it doesn't, so we
 * do it explicitly.
 */
final class ClassMonkeyVisitor extends ClassVisitor {
    private final String from, to;

    ClassMonkeyVisitor(final ClassVisitor cv, final String from, final String to) {
        super(Opcodes.ASM5, cv);
        this.from = from;
        this.to = to;
    }

    @Override
    public MethodVisitor visitMethod(int access,
                                     String name,
                                     String desc,
                                     String signature,
                                     String[] exceptions) {
        MethodVisitor visitor = super.visitMethod(access, name, desc, signature, exceptions);

        return new MethodVisitor(Opcodes.ASM5, visitor) {

            @Override
            public void visitTypeInsn(int opcode, String type) {
                if (from.equals(type))
                    super.visitTypeInsn(opcode, to);
                else
                    super.visitTypeInsn(opcode, type);
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if (opcode == Opcodes.INVOKESPECIAL && from.equals(owner) && "<init>".equals(name)) {
                    super.visitMethodInsn(opcode, to, name, desc, itf);
                } else {
                    super.visitMethodInsn(opcode, owner, name, desc, itf);
                }
            }
        };
    }

}
