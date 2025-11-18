package dev.ultreon.pyvm.compiler;

import dev.ultreon.pyvm.compiler.ast.*;
import dev.ultreon.pyvm.compiler.context.PyFileCompileContext;
import dev.ultreon.pyvm.compiler.parser.PythonParser;
import dev.ultreon.pyvm.compiler.parser.PythonParserVisitor;
import dev.ultreon.pyvm.compiler.util.PyRefType;
import dev.ultreon.pyvm.compiler.util.PyType;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.objectweb.asm.tree.ClassNode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Compiler implements PythonParserVisitor<PyNode> {
    private final ClassNode classNode;
    private final String className;
    private final String sourceFileName;
    private final Path outputDir;
    private final PyFileCompileContext context;
    private boolean annotations;

    public Compiler(String className, String sourceFileName, Path outputDir) {
        this.className = className;
        this.classNode = new ClassNode();
        classNode.superName = "java/lang/Object";
        this.sourceFileName = sourceFileName;
        this.outputDir = outputDir;
        this.context = new PyFileCompileContext(sourceFileName, className, classNode);
    }

    @Override
    public PyFileNode visitFile_input(PythonParser.File_inputContext ctx) {
        TerminalNode eof = ctx.EOF();
        PythonParser.StatementsContext statements = ctx.statements();
        if (statements != null) {
            PyStatementsNode statementsNode = visitStatements(statements);
            return new PyFileNode(statementsNode);
        }

        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitInteractive(PythonParser.InteractiveContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitEval(PythonParser.EvalContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitFunc_type(PythonParser.Func_typeContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitFstring_input(PythonParser.Fstring_inputContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyStatementsNode visitStatements(PythonParser.StatementsContext ctx) {
        List<PyStatementNode> statementNodes = new ArrayList<>();
        for (PythonParser.StatementContext statementContext : ctx.statement()) {
            PyStatementNode statementNode = visitStatement(statementContext);
            statementNodes.add(statementNode);
        }
        return new PyStatementsNode(statementNodes);
    }

    @Override
    public PyStatementNode visitStatement(PythonParser.StatementContext ctx) {
        PythonParser.Compound_stmtContext compoundStmtContext = ctx.compound_stmt();
        if (compoundStmtContext != null) return visitCompound_stmt(compoundStmtContext);
        PythonParser.Simple_stmtsContext simpleStmtsContext = ctx.simple_stmts();
        if (simpleStmtsContext != null) return visitSimple_stmts(simpleStmtsContext);
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitStatement_newline(PythonParser.Statement_newlineContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyStatementsNode visitSimple_stmts(PythonParser.Simple_stmtsContext ctx) {
        List<PythonParser.Simple_stmtContext> simpleStmtContexts = ctx.simple_stmt();
        List<PyStatementNode> statementNodes = new ArrayList<>();
        for (PythonParser.Simple_stmtContext simpleStmtContext : simpleStmtContexts) {
            PyStatementNode statementNode = visitSimple_stmt(simpleStmtContext);
            statementNodes.add(statementNode);
        }

        return new PyStatementsNode(statementNodes);
    }

    @Override
    public PyStatementNode visitSimple_stmt(PythonParser.Simple_stmtContext ctx) {
        PythonParser.Del_stmtContext delStmtContext = ctx.del_stmt();
        if (delStmtContext != null) return visitDel_stmt(delStmtContext);

        PythonParser.Import_stmtContext importStmtContext = ctx.import_stmt();
        if (importStmtContext != null) return visitImport_stmt(importStmtContext);

        PythonParser.Assert_stmtContext assertStmtContext = ctx.assert_stmt();
        if (assertStmtContext != null) return visitAssert_stmt(assertStmtContext);

        PythonParser.Global_stmtContext globalStmtContext = ctx.global_stmt();
        if (globalStmtContext != null) return visitGlobal_stmt(globalStmtContext);

        PythonParser.Raise_stmtContext raiseStmtContext = ctx.raise_stmt();
        if (raiseStmtContext != null) return visitRaise_stmt(raiseStmtContext);

        PythonParser.Nonlocal_stmtContext nonlocalStmtContext = ctx.nonlocal_stmt();
        if (nonlocalStmtContext != null) return visitNonlocal_stmt(nonlocalStmtContext);

        PythonParser.Return_stmtContext returnStmtContext = ctx.return_stmt();
        if (returnStmtContext != null) return visitReturn_stmt(returnStmtContext);

        PythonParser.Star_expressionsContext starExpressionsContext = ctx.star_expressions();
        if (starExpressionsContext != null)
            return new PyExprStatementNode(visitStar_expressions(starExpressionsContext));

        PythonParser.AssignmentContext assignment = ctx.assignment();
        if (assignment != null) return visitAssignment(assignment);

        throw new UnsupportedOperationException("Unsupported feature: " + ctx.getText());
    }

    @Override
    public PyCompoundStatementNode visitCompound_stmt(PythonParser.Compound_stmtContext ctx) {
        PythonParser.Class_defContext classDefContext = ctx.class_def();
        if (classDefContext != null) return visitClass_def(classDefContext);

        PythonParser.Function_defContext functionDefContext = ctx.function_def();
        if (functionDefContext != null) return visitFunction_def(functionDefContext);

        PythonParser.If_stmtContext ifStmtContext = ctx.if_stmt();
        if (ifStmtContext != null) return visitIf_stmt(ifStmtContext);

        PythonParser.For_stmtContext forStmtContext = ctx.for_stmt();
        if (forStmtContext != null) return visitFor_stmt(forStmtContext);

        PythonParser.Match_stmtContext matchStmtContext = ctx.match_stmt();
        if (matchStmtContext != null) return visitMatch_stmt(matchStmtContext);

        PythonParser.Try_stmtContext tryStmtContext = ctx.try_stmt();
        if (tryStmtContext != null) return visitTry_stmt(tryStmtContext);

        PythonParser.While_stmtContext whileStmtContext = ctx.while_stmt();
        if (whileStmtContext != null) return visitWhile_stmt(whileStmtContext);

        PythonParser.With_stmtContext withStmtContext = ctx.with_stmt();
        if (withStmtContext != null) return visitWith_stmt(withStmtContext);

        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyStatementNode visitAssignment(PythonParser.AssignmentContext ctx) {
        TerminalNode name = ctx.NAME();
        if (name != null) {
            annotations = true;
            PyType expr = (PyType) visitExpression(ctx.expression());
            annotations = false;
            if (expr != null) return new PyAssignNode(new PyRefNode(name.getText()), expr, visitAnnotated_rhs(ctx.annotated_rhs()));
            else throw new UnsupportedOperationException("Unsupported feature");
        } else if (!ctx.star_targets().isEmpty()) {
            if (ctx.EQUAL().size() == ctx.star_targets().size()) {
                PyTargetNode targetNode = visitStar_targets(ctx.star_targets(0));
                PythonParser.Star_expressionsContext starExpressionsContext = ctx.star_expressions();
                PythonParser.Yield_exprContext yieldExprContext = ctx.yield_expr();
                if (starExpressionsContext != null) {
                    PyExprNode starExpressionsNode = visitStar_expressions(starExpressionsContext);
                    return new PyAssignNode(targetNode, starExpressionsNode);
                }
                if (yieldExprContext != null) {
                    PyExprNode yieldExprNode = visitYield_expr(yieldExprContext);
                    return new PyAssignNode(targetNode, yieldExprNode);
                }
                throw new UnsupportedOperationException("Unsupported feature");
            }
        }

        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitAnnotated_rhs(PythonParser.Annotated_rhsContext ctx) {
        PythonParser.Star_expressionsContext starExpressionsContext = ctx.star_expressions();
        PythonParser.Yield_exprContext yieldExprContext = ctx.yield_expr();

        if (starExpressionsContext != null) return visitStar_expressions(starExpressionsContext);
        else if (yieldExprContext != null) return visitYield_expr(yieldExprContext);
        else throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitAugassign(PythonParser.AugassignContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyStatementNode visitReturn_stmt(PythonParser.Return_stmtContext ctx) {
        PythonParser.Star_expressionsContext starExpressionsContext = ctx.star_expressions();
        if (starExpressionsContext != null) return new PyReturnNode(visitStar_expressions(starExpressionsContext));
        else throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyStatementNode visitRaise_stmt(PythonParser.Raise_stmtContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyStatementNode visitGlobal_stmt(PythonParser.Global_stmtContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyStatementNode visitNonlocal_stmt(PythonParser.Nonlocal_stmtContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyStatementNode visitDel_stmt(PythonParser.Del_stmtContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyStatementNode visitYield_stmt(PythonParser.Yield_stmtContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyStatementNode visitAssert_stmt(PythonParser.Assert_stmtContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyImportNode visitImport_stmt(PythonParser.Import_stmtContext ctx) {
        PythonParser.Import_nameContext importNameContext = ctx.import_name();
        if (importNameContext != null) return visitImport_name(importNameContext);
        PythonParser.Import_fromContext importFromContext = ctx.import_from();
        if (importFromContext != null) return visitImport_from(importFromContext);
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyImportNameNode visitImport_name(PythonParser.Import_nameContext ctx) {
        PythonParser.Dotted_as_namesContext dottedAsNamesContext = ctx.dotted_as_names();
        PyDottedAsNamesNode dottedAsName = visitDotted_as_names(dottedAsNamesContext);
        return new PyImportNameNode(dottedAsName);
    }

    @Override
    public PyImportFromNode visitImport_from(PythonParser.Import_fromContext ctx) {
        PythonParser.Import_from_targetsContext importFromTargetsContext = ctx.import_from_targets();
        PythonParser.Dotted_nameContext dottedNameContext = ctx.dotted_name();
        PyDottedNameNode dottedName = visitDotted_name(dottedNameContext);
        PyImportFromTargetsNode importFromTargets = visitImport_from_targets(importFromTargetsContext);
        return new PyImportFromNode(dottedName, importFromTargets);
    }

    @Override
    public PyImportFromTargetsNode visitImport_from_targets(PythonParser.Import_from_targetsContext ctx) {
        PythonParser.Import_from_as_namesContext importFromAsNamesContext = ctx.import_from_as_names();
        PyImportFromAsNameNode[] asNameNodes = visitImport_from_as_names(importFromAsNamesContext).getAsNames();
        return new PyImportFromTargetsNode(asNameNodes);
    }

    @Override
    public PyImportFromAsNamesNode visitImport_from_as_names(PythonParser.Import_from_as_namesContext ctx) {
        List<PythonParser.Import_from_as_nameContext> importFromAsNameContexts = ctx.import_from_as_name();
        List<PyImportFromAsNameNode> asNameNodes = new ArrayList<>();
        for (PythonParser.Import_from_as_nameContext importFromAsNameContext : importFromAsNameContexts) {
            asNameNodes.add(visitImport_from_as_name(importFromAsNameContext));
        }
        return new PyImportFromAsNamesNode(asNameNodes.toArray(new PyImportFromAsNameNode[0]));
    }

    @Override
    public PyImportFromAsNameNode visitImport_from_as_name(PythonParser.Import_from_as_nameContext ctx) {
        String fromName = ctx.NAME(0).getText();
        String asName = ctx.NAME().size() > 1 ? ctx.NAME(1).getText() : null;
        return new PyImportFromAsNameNode(fromName, asName);
    }

    @Override
    public PyDottedAsNamesNode visitDotted_as_names(PythonParser.Dotted_as_namesContext ctx) {
        List<PythonParser.Dotted_as_nameContext> dottedAsNameContexts = ctx.dotted_as_name();
        List<PyDottedAsNameNode> dottedAsNameNodes = new ArrayList<>();
        for (PythonParser.Dotted_as_nameContext dottedAsNameContext : dottedAsNameContexts) {
            dottedAsNameNodes.add(visitDotted_as_name(dottedAsNameContext));
        }
        return new PyDottedAsNamesNode(dottedAsNameNodes.toArray(new PyDottedAsNameNode[0]));
    }

    @Override
    public PyDottedAsNameNode visitDotted_as_name(PythonParser.Dotted_as_nameContext ctx) {
        String dottedName = ctx.dotted_name().getText();
        if (ctx.NAME() == null) return new PyDottedAsNameNode(dottedName, null);
        String name = ctx.NAME().getText();

        return new PyDottedAsNameNode(dottedName, name);
    }

    @Override
    public PyDottedNameNode visitDotted_name(PythonParser.Dotted_nameContext ctx) {
        return new PyDottedNameNode(ctx.getText());
    }

    @Override
    public PyBlockNode visitBlock(PythonParser.BlockContext ctx) {
        PythonParser.StatementsContext statements = ctx.statements();
        if (statements != null) return new PyBlockNode(visitStatements(statements).getStatements());
        else throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitDecorators(PythonParser.DecoratorsContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyCompoundStatementNode visitClass_def(PythonParser.Class_defContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitClass_def_raw(PythonParser.Class_def_rawContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyCompoundStatementNode visitFunction_def(PythonParser.Function_defContext ctx) {
        PythonParser.Function_def_rawContext functionDefRawContext = ctx.function_def_raw();
        if (functionDefRawContext != null) return visitFunction_def_raw(functionDefRawContext);
        else throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyFunctionDefNode visitFunction_def_raw(PythonParser.Function_def_rawContext ctx) {
        PythonParser.BlockContext block = ctx.block();
        TerminalNode name = ctx.NAME();
        PythonParser.ParamsContext params = ctx.params();

        PyParamsNode paramsNode = params == null ? new PyParamsNode(new PyParamNode[0]) : visitParams(params);
        if (block != null) return new PyFunctionDefNode(name.getText(), paramsNode, visitBlock(block));
        else return new PyFunctionDefNode(name.getText(), paramsNode);
    }

    @Override
    public PyParamsNode visitParams(PythonParser.ParamsContext ctx) {
        PythonParser.ParametersContext parameters = ctx.parameters();
        if (parameters != null) return visitParameters(parameters);
        else throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyParamsNode visitParameters(PythonParser.ParametersContext ctx) {
        List<PyParamNode> params = new ArrayList<>();

        for (PythonParser.Param_no_defaultContext paramNoDefaultContext : ctx.param_no_default()) {
            params.add(visitParam_no_default(paramNoDefaultContext));
        }

        for (PythonParser.Param_with_defaultContext paramWithDefaultContext : ctx.param_with_default()) {
            System.err.println("WARNING: param_with_default not fully supported yet");
            params.add(visitParam_with_default(paramWithDefaultContext));
        }

        return new PyParamsNode(params.toArray(new PyParamNode[0]));
    }

    @Override
    public PyNode visitSlash_no_default(PythonParser.Slash_no_defaultContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitSlash_with_default(PythonParser.Slash_with_defaultContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitStar_etc(PythonParser.Star_etcContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitKwds(PythonParser.KwdsContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyParamNode visitParam_no_default(PythonParser.Param_no_defaultContext ctx) {
        PythonParser.ParamContext param = ctx.param();
        TerminalNode terminalNode = ctx.TYPE_COMMENT();
        if (terminalNode != null) throw new UnsupportedOperationException("Unsupported feature");
        if (param != null) return visitParam(param);

        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitParam_no_default_star_annotation(PythonParser.Param_no_default_star_annotationContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyParamNode visitParam_with_default(PythonParser.Param_with_defaultContext ctx) {
        return visitParam(ctx.param());
    }

    @Override
    public PyNode visitParam_maybe_default(PythonParser.Param_maybe_defaultContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyParamNode visitParam(PythonParser.ParamContext ctx) {
        TerminalNode name = ctx.NAME();
        PythonParser.AnnotationContext annotation = ctx.annotation();
        if (annotation != null) {
            PyTypeAnnotationNode node = visitAnnotation(annotation);
            return new PyTypedParamNode(name.getText(), node);
        } else {
            return new PyNamedParamNode(name.getText());
        }
    }

    @Override
    public PyNode visitParam_star_annotation(PythonParser.Param_star_annotationContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyTypeAnnotationNode visitAnnotation(PythonParser.AnnotationContext ctx) {
        PythonParser.ExpressionContext expression = ctx.expression();
        if (expression != null) {
            this.annotations = true;
            PyType pyType = (PyType) visitExpression(expression);
            this.annotations = false;
            return new PyTypeAnnotationNode(pyType);
        } else throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitStar_annotation(PythonParser.Star_annotationContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitDefault_assignment(PythonParser.Default_assignmentContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyCompoundStatementNode visitIf_stmt(PythonParser.If_stmtContext ctx) {
        PythonParser.BlockContext block = ctx.block();
        PythonParser.Named_expressionContext namedExpressionContext = ctx.named_expression();
        PythonParser.Elif_stmtContext elifStmtContext = ctx.elif_stmt();
        PythonParser.Else_blockContext elseBlockContext = ctx.else_block();

        PyBlockNode blockNode = visitBlock(block);
        PyExprNode namedExpressionNode = visitNamed_expression(namedExpressionContext);
        PyElifStmtNode elifStmtNode = elifStmtContext == null ? null : visitElif_stmt(elifStmtContext);
        PyElseBlockNode elseBlockNode = elseBlockContext == null ? null : visitElse_block(elseBlockContext);

        if (blockNode != null && namedExpressionNode != null) {
            return new PyIfStmtNode(namedExpressionNode, blockNode, elifStmtNode, elseBlockNode);
        } else throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyElifStmtNode visitElif_stmt(PythonParser.Elif_stmtContext ctx) {
        PythonParser.BlockContext block = ctx.block();
        PythonParser.Named_expressionContext namedExpressionContext = ctx.named_expression();
        PythonParser.Elif_stmtContext elifStmtContext = ctx.elif_stmt();
        PythonParser.Else_blockContext elseBlockContext = ctx.else_block();

        PyBlockNode blockNode = visitBlock(block);
        PyExprNode namedExpressionNode = visitNamed_expression(namedExpressionContext);
        PyElifStmtNode elifStmtNode = elifStmtContext == null ? null : visitElif_stmt(elifStmtContext);
        PyElseBlockNode elseBlockNode = elseBlockContext == null ? null : visitElse_block(elseBlockContext);

        if (blockNode != null && namedExpressionNode != null) {
            return new PyElifStmtNode(namedExpressionNode, blockNode, elifStmtNode, elseBlockNode);
        } else throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyElseBlockNode visitElse_block(PythonParser.Else_blockContext ctx) {
        PythonParser.BlockContext block = ctx.block();
        if (block != null) return new PyElseBlockNode(visitBlock(block).getStatements());
        else throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyCompoundStatementNode visitWhile_stmt(PythonParser.While_stmtContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyCompoundStatementNode visitFor_stmt(PythonParser.For_stmtContext ctx) {
        PythonParser.Star_expressionsContext starExpressionsContext = ctx.star_expressions();
        PythonParser.Star_targetsContext starTargetsContext = ctx.star_targets();
        PythonParser.Else_blockContext elseBlockContext = ctx.else_block();
        PythonParser.BlockContext block = ctx.block();

        return new PyForStmtNode(visitStar_targets(starTargetsContext), visitStar_expressions(starExpressionsContext), visitBlock(block), elseBlockContext == null ? null : visitElse_block(elseBlockContext));
    }

    @Override
    public PyCompoundStatementNode visitWith_stmt(PythonParser.With_stmtContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitWith_item(PythonParser.With_itemContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyCompoundStatementNode visitTry_stmt(PythonParser.Try_stmtContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyCompoundStatementNode visitExcept_block(PythonParser.Except_blockContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyCompoundStatementNode visitExcept_star_block(PythonParser.Except_star_blockContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyCompoundStatementNode visitFinally_block(PythonParser.Finally_blockContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyCompoundStatementNode visitMatch_stmt(PythonParser.Match_stmtContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitSubject_expr(PythonParser.Subject_exprContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitCase_block(PythonParser.Case_blockContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitGuard(PythonParser.GuardContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitPatterns(PythonParser.PatternsContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitPattern(PythonParser.PatternContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitAs_pattern(PythonParser.As_patternContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitOr_pattern(PythonParser.Or_patternContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitClosed_pattern(PythonParser.Closed_patternContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitLiteral_pattern(PythonParser.Literal_patternContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitLiteral_expr(PythonParser.Literal_exprContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitComplex_number(PythonParser.Complex_numberContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitSigned_number(PythonParser.Signed_numberContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitSigned_real_number(PythonParser.Signed_real_numberContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitReal_number(PythonParser.Real_numberContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitImaginary_number(PythonParser.Imaginary_numberContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitCapture_pattern(PythonParser.Capture_patternContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitPattern_capture_target(PythonParser.Pattern_capture_targetContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitWildcard_pattern(PythonParser.Wildcard_patternContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitValue_pattern(PythonParser.Value_patternContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitAttr(PythonParser.AttrContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitName_or_attr(PythonParser.Name_or_attrContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitGroup_pattern(PythonParser.Group_patternContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitSequence_pattern(PythonParser.Sequence_patternContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitOpen_sequence_pattern(PythonParser.Open_sequence_patternContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitMaybe_sequence_pattern(PythonParser.Maybe_sequence_patternContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitMaybe_star_pattern(PythonParser.Maybe_star_patternContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitStar_pattern(PythonParser.Star_patternContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitMapping_pattern(PythonParser.Mapping_patternContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitItems_pattern(PythonParser.Items_patternContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitKey_value_pattern(PythonParser.Key_value_patternContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitDouble_star_pattern(PythonParser.Double_star_patternContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitClass_pattern(PythonParser.Class_patternContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitPositional_patterns(PythonParser.Positional_patternsContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitKeyword_patterns(PythonParser.Keyword_patternsContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitKeyword_pattern(PythonParser.Keyword_patternContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitType_alias(PythonParser.Type_aliasContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitType_params(PythonParser.Type_paramsContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitType_param_seq(PythonParser.Type_param_seqContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitType_param(PythonParser.Type_paramContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitType_param_bound(PythonParser.Type_param_boundContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitExpressions(PythonParser.ExpressionsContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitExpression(PythonParser.ExpressionContext ctx) {
        PythonParser.ExpressionContext expression = ctx.expression();
        if (expression != null) throw new UnsupportedOperationException("Unsupported feature");

        TerminalNode anIf = ctx.IF();
        if (anIf != null) throw new UnsupportedOperationException("Unsupported feature");

        TerminalNode anElse = ctx.ELSE();
        if (anElse != null) throw new UnsupportedOperationException("Unsupported feature");

        PythonParser.LambdefContext lambdef = ctx.lambdef();
        if (lambdef != null) throw new UnsupportedOperationException("Unsupported feature");

        List<PyExprNode> disjunctions = new ArrayList<>();
        if (ctx.disjunction().size() == 1) return visitDisjunction(ctx.disjunction(0));
        for (PythonParser.DisjunctionContext disjunctionContext : ctx.disjunction())
            disjunctions.add(visitDisjunction(disjunctionContext));


        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitYield_expr(PythonParser.Yield_exprContext ctx) {
        TerminalNode yield = ctx.YIELD();
        if (yield != null) throw new UnsupportedOperationException("Unsupported feature");
        PythonParser.ExpressionContext expression = ctx.expression();
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitStar_expressions(PythonParser.Star_expressionsContext ctx) {
        List<PythonParser.Star_expressionContext> starExpressionContexts = ctx.star_expression();
        List<PyStarExpressionNode> starExpressionNodes = new ArrayList<>();
        if (starExpressionContexts.size() == 1) return visitStar_expression(starExpressionContexts.get(0));

        for (PythonParser.Star_expressionContext starExpressionContext : starExpressionContexts)
            starExpressionNodes.add(visitStar_expression(starExpressionContext));
        return new PyStarExpressionsNode(starExpressionNodes);
    }

    @Override
    public PyStarExpressionNode visitStar_expression(PythonParser.Star_expressionContext ctx) {
        TerminalNode star = ctx.STAR();
        return new PyStarExpressionNode(star != null, visitExpression(ctx.expression()));
    }

    @Override
    public PyStarNamedExpressionsNode visitStar_named_expressions(PythonParser.Star_named_expressionsContext ctx) {
        List<PyExprNode> starNamedExpressionNodes = new ArrayList<>();
        for (PythonParser.Star_named_expressionContext starNamedExpressionContext : ctx.star_named_expression()) {
            starNamedExpressionNodes.add(visitStar_named_expression(starNamedExpressionContext));
        }
        return new PyStarNamedExpressionsNode(starNamedExpressionNodes);

    }

    @Override
    public PyExprNode visitStar_named_expression(PythonParser.Star_named_expressionContext ctx) {
        PythonParser.Named_expressionContext namedExpressionContext = ctx.named_expression();
        TerminalNode star = ctx.STAR();

        if (star != null) throw new UnsupportedOperationException("Unsupported feature");
        return visitNamed_expression(namedExpressionContext);
    }

    @Override
    public PyAssignmentExprNode visitAssignment_expression(PythonParser.Assignment_expressionContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitNamed_expression(PythonParser.Named_expressionContext ctx) {
        PythonParser.ExpressionContext expression = ctx.expression();
        PythonParser.Assignment_expressionContext assignmentExpressionContext = ctx.assignment_expression();
        if (expression != null) return visitExpression(expression);
        else if (assignmentExpressionContext != null) return visitAssignment_expression(assignmentExpressionContext);
        else throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitDisjunction(PythonParser.DisjunctionContext ctx) {
        List<PythonParser.ConjunctionContext> conjunction = ctx.conjunction();
        PyExprNode expr = visitConjunction(conjunction.get(0));
        for (int i = 1; i < conjunction.size(); i++) expr = new PyOrNode(expr, visitConjunction(conjunction.get(i)));
        return expr;
    }

    @Override
    public PyExprNode visitConjunction(PythonParser.ConjunctionContext ctx) {
        List<PythonParser.InversionContext> inversions = ctx.inversion();
        PyExprNode expr = visitInversion(inversions.get(0));
        for (int i = 1; i < inversions.size(); i++) expr = new PyAndNode(expr, visitInversion(inversions.get(i)));
        return expr;
    }

    @Override
    public PyExprNode visitInversion(PythonParser.InversionContext ctx) {
        PythonParser.InversionContext inversion = ctx.inversion();
        if (inversion != null) throw new UnsupportedOperationException("Unsupported feature");
        if (ctx.NOT() != null) return new PyNotNode(visitComparison(ctx.comparison()));
        return visitComparison(ctx.comparison());
    }

    @Override
    public PyExprNode visitComparison(PythonParser.ComparisonContext ctx) {
        PythonParser.Bitwise_orContext bitwiseOrContext = ctx.bitwise_or();
        if (!ctx.compare_op_bitwise_or_pair().isEmpty()) {
            List<PyComparisionNode> comparisons = new ArrayList<>();
            PyExprNode expr = visitBitwise_or(bitwiseOrContext);
            for (PythonParser.Compare_op_bitwise_or_pairContext compare_op_bitwise_or_pairContext : ctx.compare_op_bitwise_or_pair()) {
                PyComparisionNode pyNode = visitCompare_op_bitwise_or_pair(compare_op_bitwise_or_pairContext);
                pyNode.setLeft(expr);
                expr = pyNode;
                comparisons.add(pyNode);
            }
            return new PyComparisonsNode(expr);
        }
        if (bitwiseOrContext != null) return visitBitwise_or(bitwiseOrContext);
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyComparisionNode visitCompare_op_bitwise_or_pair(PythonParser.Compare_op_bitwise_or_pairContext ctx) {
        PythonParser.ComparisonContext comparisonContext = (PythonParser.ComparisonContext) ctx.parent;
        PythonParser.Eq_bitwise_orContext eqBitwiseOrContext = ctx.eq_bitwise_or();
        if (eqBitwiseOrContext != null) return new PyEqualNode(visitBitwise_or(comparisonContext.bitwise_or()), visitEq_bitwise_or(eqBitwiseOrContext));
        PythonParser.Noteq_bitwise_orContext noteqBitwiseOrContext = ctx.noteq_bitwise_or();
        if (noteqBitwiseOrContext != null) return new PyNotEqualNode(visitBitwise_or(comparisonContext.bitwise_or()), visitNoteq_bitwise_or(noteqBitwiseOrContext));
        PythonParser.Gt_bitwise_orContext gtBitwiseOrContext = ctx.gt_bitwise_or();
        if (gtBitwiseOrContext != null) return new PyGreaterThanNode(visitBitwise_or(comparisonContext.bitwise_or()), visitGt_bitwise_or(gtBitwiseOrContext));
        PythonParser.Gte_bitwise_orContext gteBitwiseOrContext = ctx.gte_bitwise_or();
        if (gteBitwiseOrContext != null) return new PyGreaterThanEqualNode(visitBitwise_or(comparisonContext.bitwise_or()), visitGte_bitwise_or(gteBitwiseOrContext));
        PythonParser.Lt_bitwise_orContext ltBitwiseOrContext = ctx.lt_bitwise_or();
        if (ltBitwiseOrContext != null) return new PyLessThanNode(visitBitwise_or(comparisonContext.bitwise_or()), visitLt_bitwise_or(ltBitwiseOrContext));
        PythonParser.Lte_bitwise_orContext lteBitwiseOrContext = ctx.lte_bitwise_or();
        if (lteBitwiseOrContext != null) return new PyLessThanEqualNode(visitBitwise_or(comparisonContext.bitwise_or()), visitLte_bitwise_or(lteBitwiseOrContext));
        PythonParser.Is_bitwise_orContext isBitwiseOrContext = ctx.is_bitwise_or();
        if (isBitwiseOrContext != null) return new PyIsNode(visitBitwise_or(comparisonContext.bitwise_or()), visitIs_bitwise_or(isBitwiseOrContext));
        PythonParser.Isnot_bitwise_orContext isNotBitwiseOrContext = ctx.isnot_bitwise_or();
        if (isNotBitwiseOrContext != null) return new PyIsNotNode(visitBitwise_or(comparisonContext.bitwise_or()), visitIsnot_bitwise_or(isNotBitwiseOrContext));
        PythonParser.In_bitwise_orContext inBitwiseOrContext = ctx.in_bitwise_or();
        if (inBitwiseOrContext != null) return new PyInNode(visitBitwise_or(comparisonContext.bitwise_or()), visitIn_bitwise_or(inBitwiseOrContext));
        PythonParser.Notin_bitwise_orContext notinBitwiseOrContext = ctx.notin_bitwise_or();
        if (notinBitwiseOrContext != null) return new PyNotInNode(visitBitwise_or(comparisonContext.bitwise_or()), visitNotin_bitwise_or(notinBitwiseOrContext));
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitEq_bitwise_or(PythonParser.Eq_bitwise_orContext ctx) {
        PythonParser.Bitwise_orContext bitwiseOrContext = ctx.bitwise_or();
        if (bitwiseOrContext != null) return visitBitwise_or(bitwiseOrContext);
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitNoteq_bitwise_or(PythonParser.Noteq_bitwise_orContext ctx) {
        PythonParser.Bitwise_orContext bitwiseOrContext = ctx.bitwise_or();
        if (bitwiseOrContext != null) return visitBitwise_or(bitwiseOrContext);
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitLte_bitwise_or(PythonParser.Lte_bitwise_orContext ctx) {
        PythonParser.Bitwise_orContext bitwiseOrContext = ctx.bitwise_or();
        if (bitwiseOrContext != null) return visitBitwise_or(bitwiseOrContext);
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitLt_bitwise_or(PythonParser.Lt_bitwise_orContext ctx) {
        PythonParser.Bitwise_orContext bitwiseOrContext = ctx.bitwise_or();
        if (bitwiseOrContext != null) return visitBitwise_or(bitwiseOrContext);
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitGte_bitwise_or(PythonParser.Gte_bitwise_orContext ctx) {
        PythonParser.Bitwise_orContext bitwiseOrContext = ctx.bitwise_or();
        if (bitwiseOrContext != null) return visitBitwise_or(bitwiseOrContext);
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitGt_bitwise_or(PythonParser.Gt_bitwise_orContext ctx) {
        PythonParser.Bitwise_orContext bitwiseOrContext = ctx.bitwise_or();
        if (bitwiseOrContext != null) return visitBitwise_or(bitwiseOrContext);
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitNotin_bitwise_or(PythonParser.Notin_bitwise_orContext ctx) {
        PythonParser.Bitwise_orContext bitwiseOrContext = ctx.bitwise_or();
        if (bitwiseOrContext != null) return visitBitwise_or(bitwiseOrContext);
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitIn_bitwise_or(PythonParser.In_bitwise_orContext ctx) {
        PythonParser.Bitwise_orContext bitwiseOrContext = ctx.bitwise_or();
        if (bitwiseOrContext != null) return visitBitwise_or(bitwiseOrContext);
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitIsnot_bitwise_or(PythonParser.Isnot_bitwise_orContext ctx) {
        PythonParser.Bitwise_orContext bitwiseOrContext = ctx.bitwise_or();
        if (bitwiseOrContext != null) return visitBitwise_or(bitwiseOrContext);
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitIs_bitwise_or(PythonParser.Is_bitwise_orContext ctx) {
        PythonParser.Bitwise_orContext bitwiseOrContext = ctx.bitwise_or();
        if (bitwiseOrContext != null) return visitBitwise_or(bitwiseOrContext);
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitBitwise_or(PythonParser.Bitwise_orContext ctx) {
        PythonParser.Bitwise_orContext bitwiseOrContext = ctx.bitwise_or();
        PythonParser.Bitwise_xorContext bitwiseXorContext = ctx.bitwise_xor();

        if (bitwiseOrContext != null)
            return new PyBitwiseOrNode(visitBitwise_xor(bitwiseXorContext), visitBitwise_or(bitwiseOrContext));
        if (bitwiseXorContext != null) return visitBitwise_xor(bitwiseXorContext);
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitBitwise_xor(PythonParser.Bitwise_xorContext ctx) {
        PythonParser.Bitwise_xorContext bitwiseXorContext = ctx.bitwise_xor();
        PythonParser.Bitwise_andContext bitwiseAndContext = ctx.bitwise_and();

        if (bitwiseXorContext != null)
            return new PyBitwiseXorNode(visitBitwise_and(bitwiseAndContext), visitBitwise_xor(bitwiseXorContext));
        if (bitwiseAndContext != null) return visitBitwise_and(bitwiseAndContext);
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitBitwise_and(PythonParser.Bitwise_andContext ctx) {
        PythonParser.Bitwise_andContext bitwiseAndContext = ctx.bitwise_and();
        PythonParser.Shift_exprContext shiftexprContext = ctx.shift_expr();

        if (bitwiseAndContext != null)
            return new PyBitwiseAndNode(visitShift_expr(shiftexprContext), visitBitwise_and(bitwiseAndContext));
        if (shiftexprContext != null) return visitShift_expr(shiftexprContext);
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitShift_expr(PythonParser.Shift_exprContext ctx) {
        PythonParser.Shift_exprContext shiftexprContext = ctx.shift_expr();
        PythonParser.SumContext sum = ctx.sum();
        if (shiftexprContext != null)
            return new PyShiftExprNode(visitSum(sum), ctx.LEFTSHIFT() != null ? PyShiftExprNode.Dir.Left : PyShiftExprNode.Dir.Right, visitShift_expr(shiftexprContext));
        if (sum != null) return visitSum(sum);

        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitSum(PythonParser.SumContext ctx) {
        PythonParser.SumContext sum = ctx.sum();
        PythonParser.TermContext term = ctx.term();

        if (sum != null)
            return new PySumNode(visitTerm(term), ctx.PLUS() != null ? PySumNode.Op.Add : PySumNode.Op.Sub, visitSum(sum));
        if (term != null) return visitTerm(term);
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitTerm(PythonParser.TermContext ctx) {
        PythonParser.TermContext term = ctx.term();
        PythonParser.FactorContext factor = ctx.factor();

        if (term != null) {
            PyTermNode.Op op;
            if (ctx.STAR() != null) op = PyTermNode.Op.Mult;
            else if (ctx.SLASH() != null) op = PyTermNode.Op.Div;
            else if (ctx.PERCENT() != null) op = PyTermNode.Op.Mod;
            else if (ctx.DOUBLESLASH() != null) op = PyTermNode.Op.FloorDiv;
            else throw new UnsupportedOperationException("Unsupported feature");
            return new PyTermNode(visitFactor(factor), op, visitTerm(term));
        }
        if (factor != null) return visitFactor(factor);
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitFactor(PythonParser.FactorContext ctx) {
        PythonParser.FactorContext factor = ctx.factor();
        PythonParser.PowerContext power = ctx.power();

        if (factor != null) {
            if (ctx.PLUS() != null) return new PyUnaryOpNode(PyUnaryOpNode.Op.UAdd, visitFactor(factor));
            if (ctx.MINUS() != null) return new PyUnaryOpNode(PyUnaryOpNode.Op.USub, visitFactor(factor));
            if (ctx.TILDE() != null) return new PyUnaryOpNode(PyUnaryOpNode.Op.UNot, visitFactor(factor));
        }
        if (power != null) return visitPower(power);

        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitPower(PythonParser.PowerContext ctx) {
        PythonParser.Await_primaryContext awaitPrimaryContext = ctx.await_primary();
        PythonParser.FactorContext factor = ctx.factor();

        TerminalNode doublestar = ctx.DOUBLESTAR();
        if (doublestar != null) return new PyPowerNode(visitAwait_primary(awaitPrimaryContext), visitFactor(factor));
        if (awaitPrimaryContext != null) return visitAwait_primary(awaitPrimaryContext);
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitAwait_primary(PythonParser.Await_primaryContext ctx) {
        TerminalNode await = ctx.AWAIT();
        if (await != null) return new PyAwaitNode(visitPrimary(ctx.primary()));
        return visitPrimary(ctx.primary());
    }

    @Override
    public PyExprNode visitPrimary(PythonParser.PrimaryContext ctx) {
        PythonParser.AtomContext atom = ctx.atom();
        if (atom != null) {
            return visitAtom(atom);
        }

        PythonParser.PrimaryContext primaryContext = ctx.primary();
        if (ctx.NAME() != null) return new PyAttrNode(visitPrimary(primaryContext), ctx.NAME().getText());
        if (ctx.genexp() != null) return new PyGeneratorExprNode(visitPrimary(primaryContext), visitGenexp(ctx.genexp()));
        if (ctx.LPAR() != null) return new PyCallNode(visitPrimary(primaryContext), ctx.arguments() != null ? visitArguments(ctx.arguments()) : new PyArgumentsNode(
                new PyExprNode[0],
                null,
                new PyKeywordArgNode[0],
                null
        ));
        if (ctx.slices() != null) return new PySlicedNode(visitPrimary(primaryContext), visitSlices(ctx.slices()));
        if (ctx.primary() != null) return visitPrimary(primaryContext);

        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PySliceNode visitSlices(PythonParser.SlicesContext ctx) {
        List<PythonParser.SliceContext> slice = ctx.slice();
        if (slice.size() == 1) return visitSlice(slice.get(0));
//        List<PySliceNode> sliceNodes = new ArrayList<>();
//        for (PythonParser.SliceContext sliceContext : slice) sliceNodes.add(visitSlice(sliceContext));
//        return new PySlicesNode(sliceNodes);
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PySliceNode visitSlice(PythonParser.SliceContext ctx) {
        List<PythonParser.ExpressionContext> expression = ctx.expression();
        if (expression.size() == 1) return new PySliceNode(visitExpression(expression.get(0)), null, null);
        if (expression.size() == 2) return new PySliceNode(visitExpression(expression.get(0)), visitExpression(expression.get(1)), null);
        if (expression.size() == 3) return new PySliceNode(visitExpression(expression.get(0)), visitExpression(expression.get(1)), visitExpression(expression.get(2)));
        if (ctx.named_expression() != null) return new PySliceNode(visitNamed_expression(ctx.named_expression()), null, null);
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitAtom(PythonParser.AtomContext ctx) {
        TerminalNode name = ctx.NAME();
        if (name != null) {
            if (annotations) return new PyRefType(name.getText());
            return new PyRefNode(name.getText());
        }
        TerminalNode aTrue = ctx.TRUE();
        if (aTrue != null) return new PyBoolNode(true);
        TerminalNode aFalse = ctx.FALSE();
        if (aFalse != null) return new PyBoolNode(false);
        TerminalNode aNone = ctx.NONE();
        if (aNone != null) return new PyNoneNode();
        TerminalNode anInt = ctx.NUMBER();
        if (anInt != null) return new PyNumberNode(anInt.getText());
        PythonParser.StringsContext aString = ctx.strings();
        if (aString != null) return visitStrings(aString);
        PythonParser.TupleContext tuple = ctx.tuple();
        if (tuple != null) return visitTuple(tuple);
        PythonParser.GroupContext group = ctx.group();
        if (group != null) return visitGroup(group);
        PythonParser.GenexpContext genexp = ctx.genexp();
        if (genexp != null) return visitGenexp(genexp);
        PythonParser.ListContext list = ctx.list();
        if (list != null) return visitList(list);
        PythonParser.ListcompContext listcomp = ctx.listcomp();
        if (listcomp != null) return visitListcomp(listcomp);
        PythonParser.DictContext dict = ctx.dict();
        if (dict != null) return visitDict(dict);
        PythonParser.DictcompContext dictcomp = ctx.dictcomp();
        if (dictcomp != null) return visitDictcomp(dictcomp);
        PythonParser.SetContext set = ctx.set();
        if (set != null) return visitSet(set);
        PythonParser.SetcompContext setcomp = ctx.setcomp();
        if (setcomp != null) return visitSetcomp(setcomp);
        TerminalNode ellipsis = ctx.ELLIPSIS();
        if (ellipsis != null) return new PyEllipsisNode();

        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitGroup(PythonParser.GroupContext ctx) {
        PythonParser.Yield_exprContext yieldExprContext = ctx.yield_expr();
        if (yieldExprContext != null) return visitYield_expr(yieldExprContext);
        PythonParser.Named_expressionContext namedExpressionContext = ctx.named_expression();
        if (namedExpressionContext != null) return visitNamed_expression(namedExpressionContext);
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitLambdef(PythonParser.LambdefContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitLambda_params(PythonParser.Lambda_paramsContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitLambda_parameters(PythonParser.Lambda_parametersContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitLambda_slash_no_default(PythonParser.Lambda_slash_no_defaultContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitLambda_slash_with_default(PythonParser.Lambda_slash_with_defaultContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitLambda_star_etc(PythonParser.Lambda_star_etcContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitLambda_kwds(PythonParser.Lambda_kwdsContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitLambda_param_no_default(PythonParser.Lambda_param_no_defaultContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitLambda_param_with_default(PythonParser.Lambda_param_with_defaultContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitLambda_param_maybe_default(PythonParser.Lambda_param_maybe_defaultContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitLambda_param(PythonParser.Lambda_paramContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyFStringMiddleNode visitFstring_middle(PythonParser.Fstring_middleContext ctx) {
        PythonParser.Fstring_replacement_fieldContext fstringReplacementFieldContext = ctx.fstring_replacement_field();
        if (fstringReplacementFieldContext != null) return visitFstring_replacement_field(fstringReplacementFieldContext);
        TerminalNode terminalNode = ctx.FSTRING_MIDDLE();
        if (terminalNode != null) return new PyFStringMiddleTextNode(terminalNode.getText());
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyFStringMiddleReplacementNode visitFstring_replacement_field(PythonParser.Fstring_replacement_fieldContext ctx) {
        PythonParser.Star_expressionsContext starExpressionsContext = ctx.star_expressions();
        PythonParser.Fstring_conversionContext fstringConversionContext = ctx.fstring_conversion();
        PythonParser.Fstring_full_format_specContext fstringFullFormatSpecContext = ctx.fstring_full_format_spec();
        if (fstringConversionContext != null) throw new UnsupportedOperationException("Unsupported feature");
        if (fstringFullFormatSpecContext != null) throw new UnsupportedOperationException("Unsupported feature");

        PythonParser.Yield_exprContext yieldExprContext = ctx.yield_expr();
        if (yieldExprContext != null) throw new UnsupportedOperationException("Unsupported feature");
        if (starExpressionsContext != null) return new PyFStringMiddleReplacementNode(visitStar_expressions(starExpressionsContext));

        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitFstring_conversion(PythonParser.Fstring_conversionContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitFstring_full_format_spec(PythonParser.Fstring_full_format_specContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitFstring_format_spec(PythonParser.Fstring_format_specContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyStringNode visitFstring(PythonParser.FstringContext ctx) {
        List<PythonParser.Fstring_middleContext> fstringMiddleContexts = ctx.fstring_middle();
        List<PyFStringMiddleNode> fstringMiddleNodes = new ArrayList<>();
        for (PythonParser.Fstring_middleContext fstringMiddleContext : fstringMiddleContexts) fstringMiddleNodes.add(visitFstring_middle(fstringMiddleContext));
        return new PyFStringNode(fstringMiddleNodes);
    }

    @Override
    public PyStringNode visitString(PythonParser.StringContext ctx) {
        TerminalNode string = ctx.STRING();
        if (string != null) return new PyNaturalStringNode(string.getText().substring(1, string.getText().length() - 1));

        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyStringNode visitStrings(PythonParser.StringsContext ctx) {
        List<PyStringNode> strings = new ArrayList<>();
        for (ParseTree child : ctx.children) {
            if (child instanceof PythonParser.StringContext) {
                strings.add(visitString((PythonParser.StringContext) child));
            } else if (child instanceof PythonParser.FstringContext) {
                strings.add(visitFstring((PythonParser.FstringContext) child));
            }
        }
        return new PyStringConcatNode(strings.get(0), strings.subList(1, strings.size()));
    }

    @Override
    public PyExprNode visitList(PythonParser.ListContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitTuple(PythonParser.TupleContext ctx) {
        PythonParser.Star_named_expressionContext starNamedExpressionContext = ctx.star_named_expression();
        PythonParser.Star_named_expressionsContext starNamedExpressionsContext = ctx.star_named_expressions();
        if (starNamedExpressionsContext != null) return new PyTupleNode(visitStar_named_expression(starNamedExpressionContext), visitStar_named_expressions(starNamedExpressionsContext).getStarNamedExpressionNodes().toArray(new PyExprNode[0]));

        return new PyTupleNode(visitStar_named_expression(starNamedExpressionContext), new PyExprNode[0]);
    }

    @Override
    public PyExprNode visitSet(PythonParser.SetContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitDict(PythonParser.DictContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitDouble_starred_kvpairs(PythonParser.Double_starred_kvpairsContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitDouble_starred_kvpair(PythonParser.Double_starred_kvpairContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitKvpair(PythonParser.KvpairContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitFor_if_clauses(PythonParser.For_if_clausesContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitFor_if_clause(PythonParser.For_if_clauseContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitListcomp(PythonParser.ListcompContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitSetcomp(PythonParser.SetcompContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitGenexp(PythonParser.GenexpContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyExprNode visitDictcomp(PythonParser.DictcompContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyArgumentsNode visitArguments(PythonParser.ArgumentsContext ctx) {
        PythonParser.ArgsContext args = ctx.args();
        if (args != null) return visitArgs(args);

        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyArgumentsNode visitArgs(PythonParser.ArgsContext ctx) {
        List<PyExprNode> args = new ArrayList<>();
        for (PythonParser.ExpressionContext expressionContext : ctx.expression()) {
            args.add(visitExpression(expressionContext));
        }

        for (PythonParser.Starred_expressionContext starredExpressionContext : ctx.starred_expression()) {
            throw new UnsupportedOperationException("Unsupported feature");
        }

        PythonParser.KwargsContext kwargs = ctx.kwargs();
        List<PyKeywordArgNode> kwargsArgs = new ArrayList<>();
        if (kwargs != null) {
            kwargsArgs.addAll(visitKwargs(kwargs).getKwargs());
        }

        return new PyArgumentsNode(
                args.toArray(new PyExprNode[0]),
                null,
                kwargsArgs.toArray(new PyKeywordArgNode[0]),
                null
        );
    }

    @Override
    public PyKwargsNode visitKwargs(PythonParser.KwargsContext ctx) {
        List<PythonParser.Kwarg_or_starredContext> kwargOrStarredContexts = ctx.kwarg_or_starred();
        List<PyKeywordArgNode> kwargs = new ArrayList<>();
        for (PythonParser.Kwarg_or_starredContext kwargOrStarredContext : kwargOrStarredContexts) {
            kwargs.add(visitKwarg_or_starred(kwargOrStarredContext));
        }

        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitStarred_expression(PythonParser.Starred_expressionContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyKeywordArgNode visitKwarg_or_starred(PythonParser.Kwarg_or_starredContext ctx) {
        PythonParser.ExpressionContext expression = ctx.expression();
        TerminalNode name = ctx.NAME();
        if (expression != null) {
            if (name != null) return new PyKeywordArgNode(name.getText(), visitExpression(expression));
            throw new UnsupportedOperationException("Unsupported feature");
        }
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitKwarg_or_double_starred(PythonParser.Kwarg_or_double_starredContext ctx) {
        PythonParser.ExpressionContext expression = ctx.expression();
        TerminalNode name = ctx.NAME();
        if (expression != null) {
            if (name != null) return new PyKeywordArgNode(name.getText(), visitExpression(expression));
            throw new UnsupportedOperationException("Unsupported feature");
        }
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyTargetNode visitStar_targets(PythonParser.Star_targetsContext ctx) {
        List<PythonParser.Star_targetContext> starTargetContexts = ctx.star_target();
        List<PyTargetNode> targets = new ArrayList<>();
        for (PythonParser.Star_targetContext star_targetContext : starTargetContexts) {
            targets.add(visitStar_target(star_targetContext));
        }
        return new PyStarTargetsNode(targets.toArray(new PyTargetNode[0]));
    }

    @Override
    public PyNode visitStar_targets_list_seq(PythonParser.Star_targets_list_seqContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitStar_targets_tuple_seq(PythonParser.Star_targets_tuple_seqContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyTargetNode visitStar_target(PythonParser.Star_targetContext ctx) {
        TerminalNode star = ctx.STAR();
        if (star != null) {
            PythonParser.Star_targetContext starTargetContext = ctx.star_target();
            throw new UnsupportedOperationException("Unsupported feature");
        }
        PythonParser.Target_with_star_atomContext targetWithStarAtomContext = ctx.target_with_star_atom();
        if (targetWithStarAtomContext != null) return visitTarget_with_star_atom(targetWithStarAtomContext);
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyTargetNode visitTarget_with_star_atom(PythonParser.Target_with_star_atomContext ctx) {
        PythonParser.T_primaryContext tPrimaryContext = ctx.t_primary();
        PythonParser.Star_atomContext starAtomContext = ctx.star_atom();
        if (tPrimaryContext != null) {
            TerminalNode name = ctx.NAME();
            PythonParser.SlicesContext slices = ctx.slices();
            throw new UnsupportedOperationException("Unsupported feature (target with star atom): " + name.getText() + " " + slices.getText());
        }
        if (starAtomContext != null) return visitStar_atom(starAtomContext);
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyStarAtomNode visitStar_atom(PythonParser.Star_atomContext ctx) {
        TerminalNode name = ctx.NAME();
        if (name != null) return new PyRefNode(name.getText());
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitSingle_target(PythonParser.Single_targetContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitSingle_subscript_attribute_target(PythonParser.Single_subscript_attribute_targetContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitT_primary(PythonParser.T_primaryContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitDel_targets(PythonParser.Del_targetsContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitDel_target(PythonParser.Del_targetContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitDel_t_atom(PythonParser.Del_t_atomContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitType_expressions(PythonParser.Type_expressionsContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitFunc_type_comment(PythonParser.Func_type_commentContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitSoft_kw_type(PythonParser.Soft_kw_typeContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitSoft_kw_match(PythonParser.Soft_kw_matchContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitSoft_kw_case(PythonParser.Soft_kw_caseContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitSoft_kw_wildcard(PythonParser.Soft_kw_wildcardContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitSoft_kw__not__wildcard(PythonParser.Soft_kw__not__wildcardContext ctx) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visit(ParseTree parseTree) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitChildren(RuleNode ruleNode) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitTerminal(TerminalNode terminalNode) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    @Override
    public PyNode visitErrorNode(ErrorNode errorNode) {
        throw new UnsupportedOperationException("Unsupported feature");
    }

    public void finish() {
        context.finish(outputDir);
    }

    public PyFileCompileContext getContext() {
        return context;
    }
}
