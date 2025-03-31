package dev.ultreon.pythonc.classes

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.SymbolReferenceExpr
import dev.ultreon.pythonc.expr.MemberCallExpr
import dev.ultreon.pythonc.expr.PyExpression
import org.objectweb.asm.Type

import static org.objectweb.asm.Type.getType as typeOf

class JavaMemberCall extends MemberCallExpr {
    final theClass

    JavaMemberCall(JavaClass theClass, PyExpression parent, String name, List<PyExpression> expressions, Location location) {
        super(parent, expressions, location)
        this.theClass = theClass
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        def method = (theClass as JavaClass).functions.locate name, arguments, location
        if (parent instanceof SymbolReferenceExpr) {
            def expr = parent
            def symbol = expr.symbol()
            symbol.write compiler, writer
            for (argument in arguments) {
                (argument as PyExpression).write(compiler, writer)
            }
            writer.invokeVirtual parent.type as Type, name as String, typeOf(method) as Type, theClass.isInterface as boolean
            compiler.checkNoPop location as Location
            return
        }

        parent.write compiler, writer
        for (argument in arguments) {
            (argument as PyExpression).write(compiler, writer)
        }
        writer.invokeVirtual parent.type as Type, name as String, typeOf(method) as Type, theClass.isInterface as boolean
    }
}

class JavaConstructorCall extends MemberCallExpr {
    final theClass

    JavaConstructorCall(JavaClass theClass, PyExpression parent, List<PyExpression> arguments, Location location) {
        super(parent, arguments, location)
        this.theClass = theClass
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        def method = (theClass as JavaClass).functions.locate name, arguments, location
        if (parent instanceof SymbolReferenceExpr) {
            def expr = parent
            def symbol = expr.symbol()
            writer.newInstance symbol.type as Type, name as String, typeOf(method) as Type, theClass.isInterface as boolean, {
                for (argument in arguments) {
                    (argument as PyExpression).write(compiler, writer)
                }
            }
            compiler.checkNoPop location as Location
            return
        }

        writer.newInstance theClass.type as Type, name as String, typeOf(method) as Type, theClass.isInterface as boolean, {
            for (argument in arguments) {
                (argument as PyExpression).write(compiler, writer)
            }
        }
    }
}
