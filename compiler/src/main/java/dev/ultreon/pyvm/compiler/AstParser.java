package dev.ultreon.pyvm.compiler;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * AstParser - extended
 *
 * Single-file parser that maps nodes in the forced-serialized Python ast JSON to Java types.
 * - Robust: unknown nodes become GenericNode (preserve properties)
 * - Supports many node types found in e9c067d5-5770-4f0e-8290-091ec2d5145e.json
 *
 * Run:
 *   java -cp build/libs/yourjar.jar dev.ultreon.ast.AstParser /path/to/file.json
 *
 */
public class AstParser {

    public static void main(String[] args) throws Exception {
        String defaultPath = "pyvm/compiler/build/dist/ast.json";
        String path = args.length > 0 ? args[0] : defaultPath;
        File f = new File(path);
        if (!f.exists()) {
            System.err.println("File not found: " + f.getAbsolutePath());
            System.exit(2);
        }

        AstNode root = parse(new String(Files.readAllBytes(f.toPath())));
        if (root == null) {
            System.err.println("Failed to parse file: " + f.getAbsolutePath());
            System.exit(3);
        }

        PrettyPrinter printer = new PrettyPrinter();
        printer.print(root, System.out::println);
    }

    public static AstNode parse(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        SimpleModule mod = new SimpleModule();
        mod.addDeserializer(AstNode.class, new NodeDeserializer(AstNode.class));
        mod.addDeserializer(Op.class, new NodeDeserializer(Op.class));
        mapper.registerModule(mod);

        return mapper.readValue(json, AstNode.class);
    }

    public static AstNode parseFile(String s) throws Exception {
        return parse(Files.readString(Paths.get(s)));
    }

    // ------------------------
    // Marker interface
    // ------------------------
    public interface AstNode { }

    // ------------------------
    // Generic fallback
    // ------------------------
    public static class GenericNode implements AstNode {
        public Map<String, Object> props = new LinkedHashMap<>();
        @Override public String toString() { return "GenericNode" + props; }
    }

    // ------------------------
    // Statement / Expression node classes (expanded)
    // ------------------------
    public static class Module implements AstNode {
        public String $class;
        public List<AstNode> body = Collections.emptyList();
        public List<Object> type_ignores = Collections.emptyList();
        @Override public String toString() { return "Module"; }
    }

    public static class Expr implements AstNode {
        public String $class;
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public int lineno;
        public AstNode value;
        @Override public String toString() { return "Expr"; }
    }

    public static class FunctionDef implements AstNode {
        public String $class;
        public Arguments args;
        public List<AstNode> body = Collections.emptyList();
        public int col_offset;
        public List<Object> decorator_list = Collections.emptyList();
        public int end_col_offset;
        public int end_lineno;
        public int lineno;
        public String name;
        public AstNode returns;
        public Object type_comment;
        public List<Object> type_params = Collections.emptyList();
        @Override public String toString() { return "FunctionDef(" + name + ")"; }
    }

    public static class Arguments implements AstNode {
        public String $class;
        public List<arg> args = Collections.emptyList();
        public List<AstNode> defaults = Collections.emptyList();
        public List<AstNode> kw_defaults = Collections.emptyList();
        public AstNode kwarg;
        public List<AstNode> kwonlyargs = Collections.emptyList();
        public List<AstNode> posonlyargs = Collections.emptyList();
        public AstNode vararg;
        @Override public String toString() { return "arguments"; }
    }

    public static class Call implements AstNode {
        public String $class;
        public List<AstNode> args = Collections.emptyList();
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public AstNode func;
        public List<keyword> keywords = Collections.emptyList();
        public int lineno;
        @Override public String toString() { return "Call"; }
    }

    public static class Name implements AstNode {
        public String $class;
        public int col_offset;
        public Object ctx;
        public int end_col_offset;
        public int end_lineno;
        public String id;
        public int lineno;
        @Override public String toString() { return "Name(" + id + ")"; }
    }

    public static class Constant implements AstNode {
        public String $class;
        public Integer col_offset;
        public Integer end_col_offset;
        public Integer end_lineno;
        public Object kind;
        public Integer lineno;
        public Object n;
        public Object s;
        public Object value;
        @Override public String toString() {
            Object v = value != null ? value : (n != null ? n : s);
            return "Constant(" + v + ")";
        }
    }

    public static class Return implements AstNode {
        public String $class;
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public int lineno;
        public AstNode value;
        @Override public String toString() { return "Return"; }
    }

    public static class Assign implements AstNode {
        public String $class;
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public int lineno;
        public List<AstNode> targets = Collections.emptyList();
        public Object type_comment;
        public AstNode value;
        @Override public String toString() { return "Assign"; }
    }

    public static class AnnAssign implements AstNode {
        public String $class;
        public AstNode annotation;
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public int lineno;
        public int simple; // 0 or 1
        public AstNode target;
        public AstNode value;
        @Override public String toString() { return "AnnAssign"; }
    }

    public static class AugAssign implements AstNode {
        public String $class;
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public int lineno;
        public Op op;
        public AstNode target;
        public AstNode value;
        @Override public String toString() { return "AugAssign(" + (op!=null?op.getClass().getSimpleName():"?") + ")"; }
    }

    public static class BinOp implements AstNode {
        public String $class;
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public AstNode left;
        public Op op;
        public AstNode right;
        public int lineno;
        @Override public String toString() { return "BinOp"; }
    }

    public static class BoolOp implements AstNode {
        public String $class;
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public int lineno;
        public Op op;
        public List<AstNode> values = Collections.emptyList();
        @Override public String toString() { return "BoolOp(" + (op!=null?op.getClass().getSimpleName():"?") + ")"; }
    }

    public static class UnaryOp implements AstNode {
        public String $class;
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public Op op;
        public AstNode operand;
        public int lineno;
        @Override public String toString() { return "UnaryOp(" + (op!=null?op.getClass().getSimpleName():"?") + ")"; }
    }

    public static class If implements AstNode {
        public String $class;
        public List<AstNode> body = Collections.emptyList();
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public int lineno;
        public List<AstNode> orelse = Collections.emptyList();
        public AstNode test;
        public List<Object> keywords = Collections.emptyList();
        @Override public String toString() { return "If"; }
    }

    public static class Compare implements AstNode {
        public String $class;
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public AstNode left;
        public List<Op> ops = Collections.emptyList();
        public List<AstNode> comparators = Collections.emptyList();
        public int lineno;
        @Override public String toString() { return "Compare"; }
    }

    public static class While implements AstNode {
        public String $class;
        public List<AstNode> body = Collections.emptyList();
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public int lineno;
        public List<AstNode> orelse = Collections.emptyList();
        public AstNode test;
        @Override public String toString() { return "While"; }
    }

    public static class For implements AstNode {
        public String $class;
        public List<AstNode> body = Collections.emptyList();
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public int lineno;
        public List<AstNode> orelse = Collections.emptyList();
        public AstNode target;
        public AstNode iter;
        public Object type_comment;
        @Override public String toString() { return "For"; }
    }

    public static class Break implements AstNode { public String $class; @Override public String toString(){return "Break";}}
    public static class Continue implements AstNode { public String $class; @Override public String toString(){return "Continue";}}
    public static class Pass implements AstNode { public String $class; @Override public String toString(){return "Pass";}}

    public static class Tuple implements AstNode {
        public String $class;
        public int col_offset;
        public Object ctx;
        public List<AstNode> dims = Collections.emptyList();
        public List<AstNode> elts = Collections.emptyList();
        public int end_col_offset;
        public int end_lineno;
        public int lineno;
        @Override public String toString() { return "Tuple"; }
    }

    public static class ListNode implements AstNode { // named ListNode because List conflicts with java.util.List
        public String $class;
        public int col_offset;
        public Object ctx;
        public List<AstNode> elts = Collections.emptyList();
        public int end_col_offset;
        public int end_lineno;
        public int lineno;
        @Override public String toString() { return "List"; }
    }

    public static class Dict implements AstNode {
        public String $class;
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public List<AstNode> keys = Collections.emptyList();
        public List<AstNode> values = Collections.emptyList();
        public int lineno;
        @Override public String toString() { return "Dict"; }
    }

    public static class SetNode implements AstNode {
        public String $class;
        public int col_offset;
        public List<AstNode> elts = Collections.emptyList();
        public int end_col_offset;
        public int end_lineno;
        public int lineno;
        @Override public String toString() { return "Set"; }
    }

    // comprehensions
    public static class ListComp implements AstNode {
        public String $class;
        public AstNode elt;
        public int end_col_offset;
        public int end_lineno;
        public List<Comprehension> generators = Collections.emptyList();
        public int lineno;
        @Override public String toString() { return "ListComp"; }
    }

    public static class DictComp implements AstNode {
        public String $class;
        public AstNode key;
        public AstNode value;
        public int end_col_offset;
        public int end_lineno;
        public List<Comprehension> generators = Collections.emptyList();
        public int lineno;
        @Override public String toString() { return "DictComp"; }
    }

    public static class SetComp implements AstNode {
        public String $class;
        public AstNode elt;
        public int end_col_offset;
        public int end_lineno;
        public List<Comprehension> generators = Collections.emptyList();
        public int lineno;
        @Override public String toString() { return "SetComp"; }
    }

    public static class Comprehension implements AstNode {
        public String $class; // "comprehension"
        public List<AstNode> ifs = Collections.emptyList();
        public int is_async;
        public AstNode iter;
        public AstNode target;
        @Override public String toString() { return "comprehension"; }
    }

    // Match / match_case nodes
    public static class Match implements AstNode {
        public String $class;
        public List<AstNode> cases = Collections.emptyList();
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public int lineno;
        public AstNode subject;
        @Override public String toString() { return "Match"; }
    }

    public static class match_case implements AstNode {
        public String $class;
        public List<AstNode> body = Collections.emptyList();
        public AstNode guard; // can be null
        public AstNode pattern;
        @Override public String toString() { return "match_case"; }
    }

    public static class MatchValue implements AstNode {
        public String $class;
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public int lineno;
        public AstNode value;
        @Override public String toString() { return "MatchValue"; }
    }

    public static class MatchAs implements AstNode {
        public String $class;
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public int lineno;
        public String name; // can be null
        public AstNode pattern; // can be null
        @Override public String toString() { return "MatchAs"; }
    }

    // Exception handling, imports, etc.
    public static class Try implements AstNode {
        public String $class;
        public List<AstNode> body = Collections.emptyList();
        public List<ExceptHandler> handlers = Collections.emptyList();
        public List<AstNode> orelse = Collections.emptyList();
        public List<AstNode> finalbody = Collections.emptyList();
        @Override public String toString() { return "Try"; }
    }

    public static class ExceptHandler implements AstNode {
        public String $class; // "ExceptHandler"
        public List<AstNode> body = Collections.emptyList();
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public int lineno;
        public String name; // name can be null
        public AstNode type;
        @Override public String toString() { return "ExceptHandler"; }
    }

    public static class ImportFrom implements AstNode {
        public String $class;
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public int level;
        public int lineno;
        public String module;
        public List<Alias> names = Collections.emptyList();
        @Override public String toString() { return "ImportFrom(" + module + ")"; }
    }

    public static class Import implements AstNode {
        public String $class;
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public int lineno;
        public List<Alias> names = Collections.emptyList();

        @Override
        public String toString() {return "Import";}
    }

    public static class ClassDef implements AstNode {
        public String $class;
        public List<AstNode> bases = Collections.emptyList();
        public List<AstNode> body = Collections.emptyList();
        public int col_offset;
        public List<AstNode> decorator_list = Collections.emptyList();
        public int end_col_offset;
        public int end_lineno;
        public List<AstNode> keywords = Collections.emptyList();
        public int lineno;
        public String name;
        public List<AstNode> type_params = Collections.emptyList();

        @Override
        public String toString() {return "ClassDef(" + name + ")";}
    }

    public static class With implements AstNode {
        public String $class;
        public List<AstNode> body = Collections.emptyList();
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public int lineno;
        public List<withitem> items = Collections.emptyList();
        public Object type_comment;

        @Override
        public String toString() {return "With";}
    }

    public static class withitem implements AstNode {
        public String $class;
        public AstNode context_expr;
        public AstNode optional_vars;

        @Override
        public String toString() {return "withitem";}
    }

    public static class Assert implements AstNode {
        public String $class;
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public int lineno;
        public AstNode msg;
        public AstNode test;

        @Override
        public String toString() {return "Assert";}
    }

    public static class Delete implements AstNode {
        public String $class;
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public int lineno;
        public List<AstNode> targets = Collections.emptyList();

        @Override
        public String toString() {return "Delete";}
    }

    public static class Raise implements AstNode {
        public String $class;
        public AstNode cause;
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public AstNode exc;
        public int lineno;

        @Override
        public String toString() {return "Raise";}
    }

    public static class Lambda implements AstNode {
        public String $class;
        public Arguments args;
        public AstNode body;
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public int lineno;

        @Override
        public String toString() {return "Lambda";}
    }

    public static class Global implements AstNode {
        public String $class;
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public int lineno;
        public List<String> names = Collections.emptyList();

        @Override
        public String toString() {return "Global";}
    }

    public static class Nonlocal implements AstNode {
        public String $class;
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public int lineno;
        public List<String> names = Collections.emptyList();

        @Override
        public String toString() {return "Nonlocal";}
    }

    public static class keyword implements AstNode {
        public String $class;
        public String arg;
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public int lineno;
        public AstNode value;

        @Override
        public String toString() {return "keyword(" + arg + ")";}
    }

    public static class arg implements AstNode {
        public String $class;
        public AstNode annotation;
        public String arg;
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public int lineno;
        public Object type_comment;

        @Override
        public String toString() {return "arg(" + arg + ")";}
    }

    public static class Alias implements AstNode {
        public String $class; // "alias"
        public String asname;
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public int lineno;
        public String name;
        @Override public String toString() { return "alias(" + name + (asname!=null?(" as "+asname):"") + ")"; }
    }

    public static class Attribute implements AstNode {
        public String $class;
        public String attr;
        public int col_offset;
        public Object ctx;
        public int end_col_offset;
        public int end_lineno;
        public int lineno;
        public AstNode value;

        @Override
        public String toString() {return "Attribute(" + attr + ")";}
    }

    // ------------------------
    // Operators / Op marker classes (many kinds seen)
    // ------------------------
    public static abstract class Op implements AstNode { }
    public static class Add extends Op { public String $class; public String toString(){return "Add";}}
    public static class UAdd extends Op { public String $class; public String toString(){return "UAdd";}}
    public static class Ìnvert extends Op { public String $class; public String toString(){return "Invert";}}
    public static class Sub extends Op { public String $class; public String toString(){return "Sub";}}
    public static class USub extends Op { public String $class; public String toString(){return "USub";}}
    public static class Mult extends Op { public String $class; public String toString(){return "Mult";}}
    public static class Div extends Op { public String $class; public String toString(){return "Div";}}
    public static class FloorDiv extends Op { public String $class; public String toString(){return "FloorDiv";}}
    public static class Pow extends Op { public String $class; public String toString(){return "Pow";}}
    public static class Mod extends Op { public String $class; public String toString(){return "Mod";}}
    public static class LShift extends Op { public String $class; public String toString(){return "LShift";}}
    public static class RShift extends Op { public String $class; public String toString(){return "RShift";}}
    public static class BitAnd extends Op { public String $class; public String toString(){return "BitAnd";}}
    public static class BitXor extends Op { public String $class; public String toString(){return "BitXor";}}
    public static class BitOr extends Op { public String $class; public String toString(){return "BitOr";}}
    public static class And extends Op { public String $class; public String toString(){return "And";}}
    public static class Or extends Op { public String $class; public String toString(){return "Or";}}
    public static class Not extends Op { public String $class; public String toString(){return "Not";}}
    public static class Store extends Op { public String $class; public String toString() {return "Store";}}
    public static class Load extends Op { public String $class; public String toString() {return "Load";}}
    public static class Gt extends Op { public String $class; public String toString(){return "Gt";}}
    public static class Lt extends Op { public String $class; public String toString(){return "Lt";}}
    public static class Eq extends Op { public String $class; public String toString(){return "Eq";}}
    public static class NotEq extends Op { public String $class; public String toString(){return "NotEq";}}
    public static class GtE extends Op { public String $class; public String toString(){return "GtE";}}
    public static class LtE extends Op { public String $class; public String toString(){return "LtE";}}
    public static class Is extends Op { public String $class; public String toString(){return "Is";}}
    public static class InOp extends Op { public String $class; public String toString(){return "In";}}
    public static class NotIn extends Op { public String $class; public String toString(){return "NotIn";}}
    public static class IsNot extends Op { public String $class; public String toString(){return "IsNot";}}

    public static class Yield implements AstNode {
        public String $class;
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public int lineno;
        public AstNode value;

        @Override
        public String toString() {return "Yield";}
    }

    public static class JoinedStr implements AstNode {
        public String $class;
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public int lineno;
        public List<AstNode> values = Collections.emptyList();

        @Override
        public String toString() {return "JoinedStr";}
    }

    public static class FormattedValue implements AstNode {
        public String $class;
        public int col_offset;
        public int conversion;
        public int end_col_offset;
        public int end_lineno;
        public AstNode format_spec;
        public int lineno;
        public AstNode value;

        @Override
        public String toString() {return "FormattedValue";}
    }

    public static class Subscript implements AstNode {
        public String $class;
        public int col_offset;
        public Object ctx;
        public int end_col_offset;
        public int end_lineno;
        public int lineno;
        public AstNode slice;
        public AstNode value;

        @Override
        public String toString() {return "Subscript";}
    }

    public static class Slice implements AstNode {
        public String $class;
        public int col_offset;
        public int end_col_offset;
        public int end_lineno;
        public int lineno;
        public AstNode lower;
        public AstNode step;
        public AstNode upper;

        @Override
        public String toString() {return "Slice";}
    }

    // ------------------------
    // Deserializer
    // ------------------------
    public static class NodeDeserializer extends StdDeserializer {

        protected NodeDeserializer(Class<?> vc) { super((Class<AstNode>)vc); }

        @Override
        public AstNode deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            ObjectCodec codec = p.getCodec();
            TreeNode tree = codec.readTree(p);
            ObjectMapper mapper = (ObjectMapper) codec;

            if (!(tree instanceof ObjectNode)) {
                System.err.println("WARNING: NodeDeserializer expects ObjectNode, not " + tree.getClass());
                // primitives/arrays - convert into GenericNode
                GenericNode gn = new GenericNode();
                Map<String, Object> props = mapper.convertValue(tree, Map.class);
                gn.props.putAll(props);
                return gn;
            }
            ObjectNode node = (ObjectNode) tree;
            JsonNode clsNode = node.get("$class");
            String cls = clsNode != null ? clsNode.asText() : null;

            if (cls == null) {
                System.err.println("WARNING: NodeDeserializer expects $class to be non-null");
                GenericNode gn = new GenericNode();
                Map<String, Object> props = mapper.convertValue(node, Map.class);
                gn.props.putAll(props);
                return gn;
            }

            switch (cls) {
                case "Module": return mapper.treeToValue(node, Module.class);
                case "Expr": return mapper.treeToValue(node, Expr.class);
                case "FunctionDef": return mapper.treeToValue(node, FunctionDef.class);
                case "arguments": return mapper.treeToValue(node, Arguments.class);
                case "Call": return mapper.treeToValue(node, Call.class);
                case "Name": return mapper.treeToValue(node, Name.class);
                case "Constant": return mapper.treeToValue(node, Constant.class);
                case "Return": return mapper.treeToValue(node, Return.class);
                case "Assign": return mapper.treeToValue(node, Assign.class);
                case "AnnAssign": return mapper.treeToValue(node, AnnAssign.class);
                case "AugAssign": return mapper.treeToValue(node, AugAssign.class);
                case "BinOp": return mapper.treeToValue(node, BinOp.class);
                case "BoolOp": return mapper.treeToValue(node, BoolOp.class);
                case "UnaryOp": return mapper.treeToValue(node, UnaryOp.class);
                case "If": return mapper.treeToValue(node, If.class);
                case "Compare": return mapper.treeToValue(node, Compare.class);
                case "While": return mapper.treeToValue(node, While.class);
                case "For": return mapper.treeToValue(node, For.class);
                case "Break": return mapper.treeToValue(node, Break.class);
                case "Continue": return mapper.treeToValue(node, Continue.class);
                case "Pass": return mapper.treeToValue(node, Pass.class);
                case "Tuple": return mapper.treeToValue(node, Tuple.class);
                case "List": return mapper.treeToValue(node, ListNode.class);
                case "Dict": return mapper.treeToValue(node, Dict.class);
                case "Set": return mapper.treeToValue(node, SetNode.class);

                // comprehensions
                case "ListComp": return mapper.treeToValue(node, ListComp.class);
                case "DictComp": return mapper.treeToValue(node, DictComp.class);
                case "SetComp": return mapper.treeToValue(node, SetComp.class);
                case "comprehension": return mapper.treeToValue(node, Comprehension.class);

                // match/case
                case "Match": return mapper.treeToValue(node, Match.class);
                case "match_case": return mapper.treeToValue(node, match_case.class);
                case "MatchValue": return mapper.treeToValue(node, MatchValue.class);
                case "MatchAs": return mapper.treeToValue(node, MatchAs.class);

                // exceptions/imports
                case "Try": return mapper.treeToValue(node, Try.class);
                case "ExceptHandler": return mapper.treeToValue(node, ExceptHandler.class);
                case "ImportFrom": return mapper.treeToValue(node, ImportFrom.class);
                case "Import": return mapper.treeToValue(node, Import.class);
                case "alias": return mapper.treeToValue(node, Alias.class);
                case "ClassDef": return mapper.treeToValue(node, ClassDef.class);
                case "With": return mapper.treeToValue(node, With.class);
                case "withitem": return mapper.treeToValue(node, withitem.class);
                case "Assert": return mapper.treeToValue(node, Assert.class);
                case "Delete": return mapper.treeToValue(node, Delete.class);
                case "Raise": return mapper.treeToValue(node, Raise.class);
                case "Lambda": return mapper.treeToValue(node, Lambda.class);
                case "Global": return mapper.treeToValue(node, Global.class);
                case "Nonlocal": return mapper.treeToValue(node, Nonlocal.class);
                case "arg": return mapper.treeToValue(node, arg.class);

                // operators / comparisons
                case "Add": return mapper.treeToValue(node, Add.class);
                case "Invert": return mapper.treeToValue(node, Ìnvert.class);
                case "Sub": return mapper.treeToValue(node, Sub.class);
                case "UAdd": return mapper.treeToValue(node, UAdd.class);
                case "USub": return mapper.treeToValue(node, USub.class);
                case "Mult": return mapper.treeToValue(node, Mult.class);
                case "Div": return mapper.treeToValue(node, Div.class);
                case "FloorDiv": return mapper.treeToValue(node, FloorDiv.class);
                case "Pow": return mapper.treeToValue(node, Pow.class);
                case "Mod": return mapper.treeToValue(node, Mod.class);
                case "LShift": return mapper.treeToValue(node, LShift.class);
                case "RShift": return mapper.treeToValue(node, RShift.class);
                case "BitAnd": return mapper.treeToValue(node, BitAnd.class);
                case "BitXor": return mapper.treeToValue(node, BitXor.class);
                case "BitOr": return mapper.treeToValue(node, BitOr.class);
                case "And": return mapper.treeToValue(node, And.class);
                case "Or": return mapper.treeToValue(node, Or.class);
                case "Not": return mapper.treeToValue(node, Not.class);
                case "Store": return mapper.treeToValue(node, Store.class);
                case "Load": return mapper.treeToValue(node, Load.class);
                case "Gt": return mapper.treeToValue(node, Gt.class);
                case "Lt": return mapper.treeToValue(node, Lt.class);
                case "Eq": return mapper.treeToValue(node, Eq.class);
                case "NotEq": return mapper.treeToValue(node, NotEq.class);
                case "GtE": return mapper.treeToValue(node, GtE.class);
                case "LtE": return mapper.treeToValue(node, LtE.class);
                case "Is": return mapper.treeToValue(node, Is.class);
                case "In": return mapper.treeToValue(node, InOp.class);
                case "NotIn": return mapper.treeToValue(node, NotIn.class);
                case "IsNot": return mapper.treeToValue(node, IsNot.class);
                case "Attribute":
                    return mapper.treeToValue(node, Attribute.class);
                case "Yield":
                    return mapper.treeToValue(node, Yield.class);
                case "JoinedStr":
                    return mapper.treeToValue(node, JoinedStr.class);
                case "FormattedValue":
                    return mapper.treeToValue(node, FormattedValue.class);
                case "Subscript":
                    return mapper.treeToValue(node, Subscript.class);
                case "Slice":
                    return mapper.treeToValue(node, Slice.class);

                default:
                    System.err.println("WARNING: Unhandled case " + cls);
                    GenericNode gn = new GenericNode();
                    Map<String, Object> props = mapper.convertValue(node, Map.class);
                    gn.props.putAll(props);
                    return gn;
            }
        }
    }

    // ------------------------
    // Pretty-printer (walks and prints)
    // ------------------------
    public static class PrettyPrinter {
        private final IdentityHashMap<AstNode, Integer> seen = new IdentityHashMap<>();
        private int idCounter = 1;

        public void print(AstNode node, java.util.function.Consumer<String> out) {
            walk(node, 0, out);
        }

        private void walk(AstNode node, int indent, java.util.function.Consumer<String> out) {
            if (node == null) { out.accept(indent(indent) + "null"); return; }
            Integer id = seen.get(node);
            if (id != null) {
                out.accept(indent(indent) + "(" + node.getClass().getSimpleName() + " id#" + id + " - circular)");
                return;
            }
            seen.put(node, idCounter++);
            String base = indent(indent) + node.getClass().getSimpleName() + ": " + node.toString();
            out.accept(base);

            if (node instanceof Module) {
                Module m = (Module) node;
                for (AstNode child : safeList(m.body)) walk(child, indent+2, out);

            } else if (node instanceof Expr) {
                Expr e = (Expr) node;
                walk(e.value, indent+2, out);

            } else if (node instanceof FunctionDef) {
                FunctionDef f = (FunctionDef) node;
                out.accept(indent(indent+2) + "name: " + f.name);
                if (f.returns != null) { out.accept(indent(indent+2)+"returns:"); walk(f.returns, indent+4, out); }
                if (f.args != null) { out.accept(indent(indent+2)+"args:"); walk(f.args, indent+4, out); }
                out.accept(indent(indent+2)+"body:");
                for (AstNode ch : safeList(f.body)) walk(ch, indent+4, out);

            } else if (node instanceof Call) {
                Call c = (Call) node;
                out.accept(indent(indent+2) + "func:"); walk(c.func, indent+4, out);
                out.accept(indent(indent+2) + "args:"); for (AstNode a : safeList(c.args)) walk(a, indent+4, out);

            } else if (node instanceof Name) {
                Name n = (Name) node;
                out.accept(indent(indent+2) + "id: " + n.id);

            } else if (node instanceof Constant) {
                Constant c = (Constant) node;
                Object v = c.value != null ? c.value : (c.n != null ? c.n : c.s);
                out.accept(indent(indent+2) + "value: " + v);

            } else if (node instanceof Return) {
                Return r = (Return) node;
                out.accept(indent(indent+2) + "value:"); walk(r.value, indent+4, out);

            } else if (node instanceof Assign) {
                Assign a = (Assign) node;
                out.accept(indent(indent+2) + "targets:");
                for (AstNode t : safeList(a.targets)) walk(t, indent+4, out);
                out.accept(indent(indent+2) + "value:"); walk(a.value, indent+4, out);

            } else if (node instanceof AnnAssign) {
                AnnAssign a = (AnnAssign) node;
                out.accept(indent(indent+2) + "target:"); walk(a.target, indent+4, out);
                out.accept(indent(indent+2) + "annotation:"); walk(a.annotation, indent+4, out);
                out.accept(indent(indent+2) + "value:"); walk(a.value, indent+4, out);

            } else if (node instanceof AugAssign) {
                AugAssign a = (AugAssign) node;
                out.accept(indent(indent+2) + "op: " + (a.op!=null? a.op.toString() : "null"));
                out.accept(indent(indent+2) + "target:"); walk(a.target, indent+4, out);
                out.accept(indent(indent+2) + "value:"); walk(a.value, indent+4, out);

            } else if (node instanceof BinOp) {
                BinOp b = (BinOp) node;
                out.accept(indent(indent+2) + "left:"); walk(b.left, indent+4, out);
                out.accept(indent(indent+2) + "op: " + (b.op!=null?b.op.toString():"null"));
                out.accept(indent(indent+2) + "right:"); walk(b.right, indent+4, out);

            } else if (node instanceof BoolOp) {
                BoolOp bo = (BoolOp) node;
                out.accept(indent(indent+2) + "op: " + (bo.op!=null?bo.op.toString():"null"));
                out.accept(indent(indent+2) + "values:"); for (AstNode v : safeList(bo.values)) walk(v, indent+4, out);

            } else if (node instanceof UnaryOp) {
                UnaryOp u = (UnaryOp) node;
                out.accept(indent(indent+2) + "op: " + (u.op!=null?u.op.toString():"null"));
                out.accept(indent(indent+2) + "operand:"); walk(u.operand, indent+4, out);

            } else if (node instanceof If) {
                If iff = (If) node;
                out.accept(indent(indent+2) + "test:"); walk(iff.test, indent+4, out);
                out.accept(indent(indent+2) + "body:"); for (AstNode ch : safeList(iff.body)) walk(ch, indent+4, out);
                out.accept(indent(indent+2) + "orelse:"); for (AstNode ch : safeList(iff.orelse)) walk(ch, indent+4, out);

            } else if (node instanceof Compare) {
                Compare c = (Compare) node;
                out.accept(indent(indent+2)+"left:"); walk(c.left, indent+4, out);
                out.accept(indent(indent+2)+"ops:"); for (Op o : safeOpList(c.ops)) out.accept(indent(indent+4)+o.toString());
                out.accept(indent(indent+2)+"comparators:"); for (AstNode cmp : safeList(c.comparators)) walk(cmp, indent+4, out);

            } else if (node instanceof While) {
                While w = (While) node;
                out.accept(indent(indent+2)+"test:"); walk(w.test, indent+4, out);
                out.accept(indent(indent+2)+"body:"); for (AstNode ch : safeList(w.body)) walk(ch, indent+4, out);
                out.accept(indent(indent+2)+"orelse:"); for (AstNode ch : safeList(w.orelse)) walk(ch, indent+4, out);

            } else if (node instanceof For) {
                For f = (For) node;
                out.accept(indent(indent+2)+"target:"); walk(f.target, indent+4, out);
                out.accept(indent(indent+2)+"iter:"); walk(f.iter, indent+4, out);
                out.accept(indent(indent+2)+"body:"); for (AstNode ch : safeList(f.body)) walk(ch, indent+4, out);
                out.accept(indent(indent+2)+"orelse:"); for (AstNode ch : safeList(f.orelse)) walk(ch, indent+4, out);

            } else if (node instanceof Tuple) {
                Tuple t = (Tuple) node;
                out.accept(indent(indent+2)+"elts:"); for (AstNode e : safeList(t.elts)) walk(e, indent+4, out);

            } else if (node instanceof ListNode) {
                ListNode l = (ListNode) node;
                out.accept(indent(indent+2)+"elts:"); for (AstNode e : safeList(l.elts)) walk(e, indent+4, out);

            } else if (node instanceof Dict) {
                Dict d = (Dict) node;
                out.accept(indent(indent+2)+"keys:"); for (AstNode k : safeList(d.keys)) walk(k, indent+4, out);
                out.accept(indent(indent+2)+"values:"); for (AstNode v : safeList(d.values)) walk(v, indent+4, out);

            } else if (node instanceof SetNode) {
                SetNode s = (SetNode) node;
                out.accept(indent(indent+2)+"elts:"); for (AstNode e : safeList(s.elts)) walk(e, indent+4, out);

            } else if (node instanceof ListComp) {
                ListComp lc = (ListComp) node;
                out.accept(indent(indent+2)+"elt:"); walk(lc.elt, indent+4, out);
                out.accept(indent(indent+2)+"generators:"); for (Comprehension c : safeCompList(lc.generators)) walk(c, indent+4, out);

            } else if (node instanceof DictComp) {
                DictComp dc = (DictComp) node;
                out.accept(indent(indent+2)+"key:"); walk(dc.key, indent+4, out);
                out.accept(indent(indent+2)+"value:"); walk(dc.value, indent+4, out);
                out.accept(indent(indent+2)+"generators:"); for (Comprehension c : safeCompList(dc.generators)) walk(c, indent+4, out);

            } else if (node instanceof SetComp) {
                SetComp sc = (SetComp) node;
                out.accept(indent(indent+2)+"elt:"); walk(sc.elt, indent+4, out);
                out.accept(indent(indent+2)+"generators:"); for (Comprehension c : safeCompList(sc.generators)) walk(c, indent+4, out);

            } else if (node instanceof Comprehension) {
                Comprehension c = (Comprehension) node;
                out.accept(indent(indent+2)+"target:"); walk(c.target, indent+4, out);
                out.accept(indent(indent+2)+"iter:"); walk(c.iter, indent+4, out);
                out.accept(indent(indent+2)+"ifs:"); for (AstNode ifn : safeList(c.ifs)) walk(ifn, indent+4, out);
                out.accept(indent(indent+2)+"is_async: " + c.is_async);

            } else if (node instanceof Match) {
                Match m = (Match) node;
                out.accept(indent(indent+2)+"subject:"); walk(m.subject, indent+4, out);
                out.accept(indent(indent+2)+"cases:"); for (AstNode c : safeList(m.cases)) walk(c, indent+4, out);

            } else if (node instanceof match_case) {
                match_case mc = (match_case) node;
                out.accept(indent(indent+2)+"pattern:"); walk(mc.pattern, indent+4, out);
                out.accept(indent(indent+2)+"guard:"); walk(mc.guard, indent+4, out);
                out.accept(indent(indent+2)+"body:"); for (AstNode b : safeList(mc.body)) walk(b, indent+4, out);

            } else if (node instanceof MatchValue) {
                MatchValue mv = (MatchValue) node;
                out.accept(indent(indent+2)+"value:"); walk(mv.value, indent+4, out);

            } else if (node instanceof MatchAs) {
                MatchAs ma = (MatchAs) node;
                out.accept(indent(indent+2)+"name: " + ma.name);
                out.accept(indent(indent+2)+"pattern:"); walk(ma.pattern, indent+4, out);

            } else if (node instanceof Try) {
                Try t = (Try) node;
                out.accept(indent(indent+2)+"body:"); for (AstNode b : safeList(t.body)) walk(b, indent+4, out);
                out.accept(indent(indent+2)+"handlers:"); for (ExceptHandler h : safeExceptList(t.handlers)) walk(h, indent+4, out);
                out.accept(indent(indent+2)+"orelse:"); for (AstNode b : safeList(t.orelse)) walk(b, indent+4, out);
                out.accept(indent(indent+2)+"finalbody:"); for (AstNode b : safeList(t.finalbody)) walk(b, indent+4, out);

            } else if (node instanceof ExceptHandler) {
                ExceptHandler h = (ExceptHandler) node;
                out.accept(indent(indent+2)+"type:"); walk(h.type, indent+4, out);
                out.accept(indent(indent+2)+"name: " + h.name);
                out.accept(indent(indent+2)+"body:"); for (AstNode b : safeList(h.body)) walk(b, indent+4, out);

            } else if (node instanceof ImportFrom) {
                ImportFrom im = (ImportFrom) node;
                out.accept(indent(indent+2)+"module: " + im.module);
                out.accept(indent(indent+2)+"names:"); for (Alias a : safeAliasList(im.names)) walk(a, indent+4, out);

            } else if (node instanceof Alias) {
                Alias a = (Alias) node;
                out.accept(indent(indent+2)+"name: " + a.name + (a.asname!=null?(" as "+a.asname):""));

            } else if (node instanceof ClassDef) {
                ClassDef c = (ClassDef) node;
                out.accept(indent(indent + 2) + "name: " + c.name);
                out.accept(indent(indent + 2) + "bases:");
                for (AstNode b : safeList(c.bases)) walk(b, indent + 4, out);
                out.accept(indent(indent + 2) + "body:");
                for (AstNode b : safeList(c.body)) walk(b, indent + 4, out);

            } else if (node instanceof With) {
                With w = (With) node;
                out.accept(indent(indent + 2) + "items:");
                for (withitem item : safeWithItemList(w.items)) walk(item, indent + 4, out);
                out.accept(indent(indent + 2) + "body:");
                for (AstNode b : safeList(w.body)) walk(b, indent + 4, out);

            } else if (node instanceof withitem) {
                withitem wi = (withitem) node;
                out.accept(indent(indent + 2) + "context_expr:");
                walk(wi.context_expr, indent + 4, out);
                out.accept(indent(indent + 2) + "optional_vars:");
                walk(wi.optional_vars, indent + 4, out);

            } else if (node instanceof Raise) {
                Raise r = (Raise) node;
                out.accept(indent(indent + 2) + "exc:");
                walk(r.exc, indent + 4, out);
                out.accept(indent(indent + 2) + "cause:");
                walk(r.cause, indent + 4, out);

            } else if (node instanceof GenericNode) {
                GenericNode gn = (GenericNode) node;
                for (Map.Entry<String,Object> e : gn.props.entrySet()) {
                    out.accept(indent(indent+2) + e.getKey() + ": " + (e.getValue()!=null ? e.getValue().toString() : "null"));
                }

            } else if (node instanceof Nonlocal) {
                Nonlocal n = (Nonlocal) node;
                out.accept(indent(indent + 2) + "names:");
                for (String name : n.names) {
                    out.accept(indent(indent + 4) + name);
                }

            } else if (node instanceof keyword) {
                keyword k = (keyword) node;
                out.accept(indent(indent + 2) + "arg: " + k.arg);
                out.accept(indent(indent + 2) + "value:");
                walk(k.value, indent + 4, out);

            } else if (node instanceof Attribute) {
                Attribute a = (Attribute) node;
                out.accept(indent(indent + 2) + "attr: " + a.attr);
                out.accept(indent(indent + 2) + "value:");
                walk(a.value, indent + 4, out);

            } else if (node instanceof Yield) {
                Yield y = (Yield) node;
                out.accept(indent(indent + 2) + "value:");
                walk(y.value, indent + 4, out);
            } else if (node instanceof JoinedStr) {
                JoinedStr js = (JoinedStr) node;
                out.accept(indent(indent + 2) + "values:");
                for (AstNode value : safeList(js.values)) {
                    walk(value, indent + 4, out);
                }
            } else if (node instanceof FormattedValue) {
                FormattedValue fv = (FormattedValue) node;
                out.accept(indent(indent + 2) + "value:");
                walk(fv.value, indent + 4, out);
                out.accept(indent(indent + 2) + "conversion: " + fv.conversion);
                out.accept(indent(indent + 2) + "format_spec:");
                walk(fv.format_spec, indent + 4, out);
            } else if (node instanceof Subscript) {
                Subscript s = (Subscript) node;
                out.accept(indent(indent + 2) + "value:");
                walk(s.value, indent + 4, out);
                out.accept(indent(indent + 2) + "slice:");
                walk(s.slice, indent + 4, out);
            } else if (node instanceof Slice) {
                Slice s = (Slice) node;
                out.accept(indent(indent + 2) + "lower:");
                walk(s.lower, indent + 4, out);
                out.accept(indent(indent + 2) + "upper:");
                walk(s.upper, indent + 4, out);
                out.accept(indent(indent + 2) + "step:");
                walk(s.step, indent + 4, out);
            } else {
                ObjectMapper m = new ObjectMapper();
                Map<String,Object> props = m.convertValue(node, Map.class);
                for (Map.Entry<String,Object> e : props.entrySet()) {
                    out.accept(indent(indent+2) + e.getKey() + ": " + (e.getValue()!=null ? e.getValue().toString() : "null"));
                }
            }
        }

        private List<AstNode> safeList(List<AstNode> l) { return l == null ? Collections.emptyList() : l; }
        private List<Comprehension> safeCompList(List<Comprehension> l) { return l == null ? Collections.emptyList() : l; }
        private List<ExceptHandler> safeExceptList(List<ExceptHandler> l) { return l == null ? Collections.emptyList() : l; }
        private List<Alias> safeAliasList(List<Alias> l) { return l == null ? Collections.emptyList() : l; }
        private List<Op> safeOpList(List<Op> l) { return l == null ? Collections.emptyList() : l; }

        private List<withitem> safeWithItemList(List<withitem> l) {return l == null ? Collections.emptyList() : l;}

        private String indent(int i) {
            char[] c = new char[i];
            Arrays.fill(c, ' ');
            return new String(c);
        }
    }
}
