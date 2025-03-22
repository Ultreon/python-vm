package dev.ultreon.pythonc;

import org.objectweb.asm.Label;

public interface LoopContext extends Context {

    Label getContinuationLabel();

    Label getBreakLabel();
}
