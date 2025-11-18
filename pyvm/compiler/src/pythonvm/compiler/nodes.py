from abc import ABCMeta

from .context import *


class Node(ABC):
    pass

class ModuleNode(Node):
    def __init__(self, stmts: list[Node]):
        self.stmts = stmts


class FunctionDefNode(Node):
    def __init__(self, name: str, body: Node, args: Node, returns: Node):
        self.name = name
        self.body = body
        self.args = args
        self.returns = returns

    def compile(self, method: java.Method, context: Context):
        raise NotImplemented


class ExprNode(Node, metaclass=ABCMeta):
    @property
    @abstractmethod
    def resolved_type(self) -> Type:
        pass


class ReturnNode(ExprNode):
    def __init__(self, expr: ExprNode):
        self.expr = expr

    @property
    def resolved_type(self) -> Type:
        return self.expr.resolved_type


class KwargNode(Node):
    def __init__(self, name: str, expr: ExprNode):
        self.name = name
        self.expr = expr

    @property
    def resolved_type(self) -> Type:
        return self.expr.resolved_type


class CallNode(ExprNode):
    def __init__(self, func: ExprNode, args: list[ExprNode], keywords: list[KwargNode]):
        self.func = func
        self.args = args
        self.keywords = keywords

    @property
    def resolved_type(self) -> Type:
        return self.func.resolved_type


class RefNode(ExprNode):
    def __init__(self, name: str):
        self.name = name

    @property
    def resolved_type(self) -> Type:
        return OBJECT_TYPE


class ConstantNode(ExprNode):
    def __init__(self, value):
        self.value = value
