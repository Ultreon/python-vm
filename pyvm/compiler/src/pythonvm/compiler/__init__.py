from pathlib import Path
from typing import Dict, Any

import voc.java as java
import ast as python
from abc import ABC, abstractmethod
from .type import *
from .nodes import *


class Compiler:
    def __init__(self, context: FileContext):
        self.context = context

    def parse(self, ast: python.AST) -> Node:
        func_name = f"parse_{ast.__class__.__name__.lower()}"
        if hasattr(self, func_name):
            node = getattr(self, func_name)(ast)
            if not isinstance(node, Node):
                raise ValueError(f"Expected {Node.__name__}, got {node.__class__.__name__}")
            return node
        else:
            raise NotImplementedError(f"Unimplemented AST: {ast.__class__.__name__}")

    def parse_module(self, ast: python.Module):
        stmts = []
        for stmt in ast.body:
            stmts.append(self.parse(stmt))
        return ModuleNode(stmts)

    def parse_functiondef(self, ast: python.FunctionDef) -> Node:
        return FunctionDefNode(ast.name, self.parse(ast.body), self.parse(ast.args), self.parse(ast.returns))

    def parse_expr(self, ast: python.Expr) -> ExprNode:
        expr = self.parse(ast.value)
        if not isinstance(expr, ExprNode):
            raise ValueError(f"Expected {ExprNode.__name__}, got {expr.__class__.__name__}")
        return expr

    def parse_call(self, ast: python.Call) -> ExprNode:
        return CallNode(self.parse(ast.func).name, [self.parse(arg) for arg in ast.args],
                        [self.parse(kwarg) for kwarg in ast.keywords])

    def parse_name(self, ast: python.Name) -> ExprNode:
        return RefNode(ast.id)

    def parse_keyword(self, ast: python.keyword) -> ExprNode:
        return KeywordNode(ast.arg, self.parse(ast.value))

    def parse_constant(self, ast: python.Constant) -> ExprNode:
        return ConstantNode(ast.value)

    def compile(self, node: Node):
        node.compile(self.context.method, self.context)


def parse(ast: python.Module, filename: str, output_dir: str, package: str) -> Node:
    context = FileContext(package, filename)
    compiler = Compiler(context)
    compiler.parse(ast)
    java.write_class(context.__klass, output_dir)


def serialize_all(o: object) -> Dict[str, Any]:
    d = {}
    d["$class"] = o.__class__.__name__
    for key in dir(o):
        if key.startswith("_"):
            continue
        value = getattr(o, key)
        if callable(value):
            continue
        d[key] = value
    return d


def main(input_file: str, output_dir: str, package: str) -> int:
    with open(input_file, "r") as f:
        ast = python.parse(f.read())

    import json
    import os

    print(output_dir)
    if not os.path.exists(Path(output_dir).parent):
        os.makedirs(Path(output_dir).parent, exist_ok=True)

    with open(output_dir, "w") as f:
        json.dump(ast, f, default=serialize_all, indent=2)

    os.system(f"java -cp {os.environ['PYVM_COMPILER']}{os.pathsep}{os.environ['PYVM_CLASSPATH']} {output_dir}/ast.json {output_dir}/classes/")
    return 0


if __name__ == "__main__":
    import sys

    sys.exit(main(*sys.argv[1:]))
