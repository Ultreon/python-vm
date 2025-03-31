package dev.ultreon.pythonc.expr.fstring

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.StarExpression
import dev.ultreon.pythonc.expr.PyExpression

abstract class FStringElement extends PyExpression {
    FStringElement(Location location) {
        super(location)
    }

    static Replacement replacement(starExpressions, location) {
        return new Replacement(starExpressions as List, location as Location)
    }

    static FStringElement text(text, location) {
        return new Text(text as String, location as Location)
    }

    static class Text extends FStringElement {
        String text

        Text(String text, Location location) {
            super(location)
            this.text = text
        }

        @Override
        void writeCode(PythonCompiler compiler, JvmWriter writer) {
            writer.loadConstant text
        }
    }

    static class Replacement extends FStringElement {
        List<StarExpression> starExpressions

        Replacement(List<StarExpression> starExpressions, Location location) {
            super(location)
            this.starExpressions = starExpressions
        }

        @Override
        void writeCode(PythonCompiler compiler, JvmWriter writer) {
            for (expr in starExpressions) {
                expr.write compiler, writer
                compiler.checkNoPop(location)
            }
        }
    }
}
