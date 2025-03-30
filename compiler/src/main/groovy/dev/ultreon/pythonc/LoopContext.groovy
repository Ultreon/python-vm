package dev.ultreon.pythonc;

import org.objectweb.asm.Label;

interface LoopContext extends Context {

    Label getContinuationLabel();

    Label getBreakLabel();
}
