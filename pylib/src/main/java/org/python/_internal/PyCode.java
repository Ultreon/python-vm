package org.python._internal;

import java.util.Objects;

public class PyCode {
    public static final int CO_OPTIMIZED    = 0x0001;
    public static final int CO_NEWLOCALS    = 0x0002;
    public static final int CO_VARARGS      = 0x0004;
    public static final int CO_VARKEYWORDS  = 0x0008;
    public static final int CO_NESTED       = 0x0010;
    public static final int CO_GENERATOR    = 0x0020;
    public static final int CO_COROUTINE    = 0x0080;
    public static final int CO_ASYNC_GENERATOR = 0x0200;
    public final int co_argcount;
    public final int co_posonlyargcount;
    public final int co_kwonlyargcount;
    public final int co_nlocals;
    public final String[] co_varnames;
    public final String co_filename;
    public final String co_name;
    public final int co_firstlineno;
    public final int co_flags;
    public final Object[] co_consts;
    public final byte[] co_code; // Stays null.

    private PyCode(Builder builder) {
        this.co_argcount = builder.argCount;
        this.co_posonlyargcount = builder.posOnlyArgCount;
        this.co_kwonlyargcount = builder.kwOnlyArgCount;
        this.co_nlocals = builder.nLocals;
        this.co_varnames = builder.varNames;
        this.co_filename = builder.filename;
        this.co_name = builder.name;
        this.co_firstlineno = builder.firstLineNo;
        this.co_flags = builder.flags;
        this.co_consts = builder.consts;
        this.co_code = null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int argCount;
        private int posOnlyArgCount;
        private int kwOnlyArgCount;
        private int nLocals;
        private String[] varNames = new String[0];
        private String filename = "";
        private String name = "";
        private int firstLineNo;
        private int flags;
        private Object[] consts = new Object[0];
        private int stackSize;

        public Builder argCount(int argCount) {
            this.argCount = argCount;
            return this;
        }

        public Builder posOnlyArgCount(int posOnlyArgCount) {
            this.posOnlyArgCount = posOnlyArgCount;
            return this;
        }

        public Builder kwOnlyArgCount(int kwOnlyArgCount) {
            this.kwOnlyArgCount = kwOnlyArgCount;
            return this;
        }

        public Builder nLocals(int nLocals) {
            this.nLocals = nLocals;
            return this;
        }

        public Builder varNames(String... varNames) {
            this.varNames = varNames;
            return this;
        }

        public Builder filename(String filename) {
            this.filename = filename;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder firstLineNo(int firstLineNo) {
            this.firstLineNo = firstLineNo;
            return this;
        }

        public Builder flags(int flags) {
            this.flags = flags;
            return this;
        }

        public Builder consts(Object... consts) {
            this.consts = consts;
            return this;
        }

        public PyCode build() {
            return new PyCode(this);
        }

        public Builder stackSize(int stackSize) {
            this.stackSize = stackSize;
            return this;
        }
    }

    @Override
    public String toString() {
        return String.format(
                "<code object %s at 0x%s, file \"%s\", line %d>",
                co_name,
                Integer.toHexString(Objects.hashCode(this)),
                co_filename,
                co_firstlineno
        );
    }
}