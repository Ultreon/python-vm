package dev.ultreon.pythonc.fields


import dev.ultreon.pythonc.lang.PyAST
import org.objectweb.asm.Type

trait JvmField implements PyAST {
    abstract Type getType()
}