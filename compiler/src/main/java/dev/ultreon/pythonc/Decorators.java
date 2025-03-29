package dev.ultreon.pythonc;

import dev.ultreon.pythonc.expr.MemberCallExpr;

import java.util.HashMap;
import java.util.Map;

class Decorators {
    public final Map<String, MemberCallExpr> byJvmName = new HashMap<>();
}
