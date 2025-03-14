/*
Python grammar
The MIT License (MIT)
Copyright (c) 2021 Robert Einhorn

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

 /*
  * Project      : an ANTLR4 parser grammar by the official PEG grammar
  *                https://github.com/RobEin/ANTLR4-parser-for-Python-3.12
  * Developed by : Robert Einhorn
  *
  */

  /*
   * Contributors :
   * [Willie Shen](https://github.com/Willie169) : Fix that `case [a, *_] if a == 0:` throws error `rule soft_kw__not__wildcard failed predicate: {this.isnotEqualToCurrentTokenText("_")}?`
  */

parser grammar PythonParser; // Python 3.12.6  https://docs.python.org/3.12/reference/grammar.html#full-grammar-specification
options {
    tokenVocab=PythonLexer;
    superClass=PythonParserBase;
}

// STARTING RULES
// ==============

file_input: statements? EOF;
interactive: statement_newline;
eval: expressions NEWLINE* EOF;
func_type: LPAR type_expressions? RPAREN RPAR expression NEWLINE* EOF;
fstring_input: star_expressions;

// GENERAL STATEMENTS
// ==================

statements: statement+;

statement: compound_stmt  | simple_stmts;

statement_newline
    : compound_stmt NEWLINE
    | simple_stmts
    | NEWLINE
    | EOF;

simple_stmts
    : simple_stmt (SEMI simple_stmt)* SEMI? NEWLINE
    ;

// NOTE: assignment MUST precede expression, else parsing a simple assignment
// will throw a SyntaxError.
simple_stmt
    : assignment
    | type_alias
    | star_expressions
    | return_stmt
    | import_stmt
    | raise_stmt
    | PASS
    | del_stmt
    | yield_stmt
    | assert_stmt
    | BREAK
    | CONTINUE
    | global_stmt
    | nonlocal_stmt;

compound_stmt
    : function_def
    | if_stmt
    | class_def
    | with_stmt
    | for_stmt
    | try_stmt
    | while_stmt
    | match_stmt;

// SIMPLE STATEMENTS
// =================

// NOTE: annotated_rhs may start with 'yield'; yield_expr must start with 'yield'
assignment
    : NAME COLON expression (EQUAL annotated_rhs )?
    | (LPAR single_target RPAR
         | single_subscript_attribute_target) COLON expression (EQUAL annotated_rhs )?
    | (star_targets EQUAL )+ (yield_expr | star_expressions) TYPE_COMMENT?
    | single_target augassign (yield_expr | star_expressions);

annotated_rhs: yield_expr | star_expressions;

augassign
    : PLUSEQUAL
    | MINUSEQUAL
    | STAREQUAL
    | ATEQUAL
    | SLASHEQUAL
    | PERCENTEQUAL
    | AMPEREQUAL
    | VBAREQUAL
    | CIRCUMFLEXEQUAL
    | LEFTSHIFTEQUAL
    | RIGHTSHIFTEQUAL
    | DOUBLESTAREQUAL
    | DOUBLESLASHEQUAL;

return_stmt
    : RETURN star_expressions?;

raise_stmt
    : RAISE (expression (FROM expression )?)?
    ;

global_stmt: GLOBAL NAME (COMMA NAME)*;

nonlocal_stmt: NONLOCAL NAME (COMMA NAME)*;

del_stmt
    : DEL del_targets;

yield_stmt: yield_expr;

assert_stmt: ASSERT expression (COMMA expression )?;

import_stmt
    : import_name
    | import_from;

// Import statements
// -----------------

import_name: IMPORT dotted_as_names;
// note below: the (DOT | '...') is necessary because '...' is tokenized as ELLIPSIS
import_from
    : FROM (DOT | ELLIPSIS)* dotted_name IMPORT import_from_targets
    | FROM (DOT | ELLIPSIS)+ IMPORT import_from_targets;
import_from_targets
    : LPAR import_from_as_names COMMA? RPAR
    | import_from_as_names
    | STAR;
import_from_as_names
    : import_from_as_name (COMMA import_from_as_name)*;
import_from_as_name
    : NAME (AS NAME )?;
dotted_as_names
    : dotted_as_name (COMMA dotted_as_name)*;
dotted_as_name
    : dotted_name (AS NAME )?;
dotted_name
    : dotted_name DOT NAME
    | NAME;

// COMPOUND STATEMENTS
// ===================

// Common elements
// ---------------

block
    : NEWLINE INDENT statements DEDENT
    | simple_stmts;

decorators: (AT named_expression NEWLINE )+;

// Class definitions
// -----------------

class_def
    : decorators class_def_raw
    | class_def_raw;

class_def_raw
    : CLASS NAME type_params? (LPAR arguments? RPAR )? COLON block;

// Function definitions
// --------------------

function_def
    : decorators function_def_raw
    | function_def_raw;

function_def_raw
    : DEF NAME type_params? LPAR params? RPAR (RARROW expression )? COLON func_type_comment? block
    | ASYNC DEF NAME type_params? LPAR params? RPAR (RARROW expression )? COLON func_type_comment? block;

// Function parameters
// -------------------

params
    : parameters;

parameters
    : slash_no_default param_no_default* param_with_default* star_etc?
    | slash_with_default param_with_default* star_etc?
    | param_no_default+ param_with_default* star_etc?
    | param_with_default+ star_etc?
    | star_etc;

// Some duplication here because we can't write (COMMA | {isCurrentTokenType(RPAR)}?),
// which is because we don't support empty alternatives (yet).

slash_no_default
    : param_no_default+ SLASH COMMA?
    ;
slash_with_default
    : param_no_default* param_with_default+ SLASH COMMA?
    ;

star_etc
    : STAR param_no_default param_maybe_default* kwds?
    | STAR param_no_default_star_annotation param_maybe_default* kwds?
    | STAR COMMA param_maybe_default+ kwds?
    | kwds;

kwds
    : DOUBLESTAR param_no_default;

// One parameter.  This *includes* a following comma and type comment.
//
// There are three styles:
// - No default_assignment
// - With default_assignment
// - Maybe with default_assignment
//
// There are two alternative forms of each, to deal with type comments:
// - Ends in a comma followed by an optional type comment
// - No comma, optional type comment, must be followed by close paren
// The latter form is for a final parameter without trailing comma.
//

param_no_default
    : param COMMA? TYPE_COMMENT?
    ;
param_no_default_star_annotation
    : param_star_annotation COMMA? TYPE_COMMENT?
    ;
param_with_default
    : param default_assignment COMMA? TYPE_COMMENT?
    ;
param_maybe_default
    : param default_assignment? COMMA? TYPE_COMMENT?
    ;
param: NAME annotation?;
param_star_annotation: NAME star_annotation;
annotation: COLON expression;
star_annotation: COLON star_expression;
default_assignment: EQUAL expression;

// If statement
// ------------

if_stmt
    : IF named_expression COLON block (elif_stmt | else_block?)
    ;
elif_stmt
    : ELIF named_expression COLON block (elif_stmt | else_block?)
    ;
else_block
    : ELSE COLON block;

// While statement
// ---------------

while_stmt
    : WHILE named_expression COLON block else_block?;

// For statement
// -------------

for_stmt
    : ASYNC? FOR star_targets IN star_expressions COLON TYPE_COMMENT? block else_block?
    ;

// With statement
// --------------

with_stmt
    : ASYNC? WITH ( LPAR with_item (COMMA with_item)* COMMA? RPAR COLON
                    | with_item (COMMA with_item)* COLON TYPE_COMMENT?
                    ) block
    ;

with_item
    : expression (AS star_target)?
    ;

// Try statement
// -------------

try_stmt
    : TRY COLON block finally_block
    | TRY COLON block except_block+ else_block? finally_block?
    | TRY COLON block except_star_block+ else_block? finally_block?;


// Except statement
// ----------------

except_block
    : EXCEPT (expression (AS NAME )?)? COLON block
    ;
except_star_block
    : EXCEPT STAR expression (AS NAME )? COLON block;
finally_block
    : FINALLY COLON block;

// Match statement
// ---------------

match_stmt
    : soft_kw_match subject_expr COLON NEWLINE INDENT case_block+ DEDENT;

subject_expr
    : star_named_expression COMMA star_named_expressions?
    | named_expression;

case_block
    : soft_kw_case patterns guard? COLON block;

guard: IF named_expression;

patterns
    : open_sequence_pattern
    | pattern;

pattern
    : as_pattern
    | or_pattern;

as_pattern
    : or_pattern AS pattern_capture_target;

or_pattern
    : closed_pattern (VBAR closed_pattern)*;

closed_pattern
    : literal_pattern
    | capture_pattern
    | wildcard_pattern
    | value_pattern
    | group_pattern
    | sequence_pattern
    | mapping_pattern
    | class_pattern;

// Literal patterns are used for equality and identity constraints
literal_pattern
    : signed_number
    | complex_number
    | strings
    | NONE
    | TRUE
    | FALSE;

// Literal expressions are used to restrict permitted mapping pattern keys
literal_expr
    : signed_number
    | complex_number
    | strings
    | NONE
    | TRUE
    | FALSE;

complex_number
    : signed_real_number (PLUS | MINUS) imaginary_number
    ;

signed_number
    : MINUS? NUMBER
    ;

signed_real_number
    : MINUS? real_number
    ;

real_number
    : NUMBER;

imaginary_number
    : NUMBER;

capture_pattern
    : pattern_capture_target;

pattern_capture_target
    : soft_kw__not__wildcard;

wildcard_pattern
    : soft_kw_wildcard;

value_pattern
    : attr;

attr
    : NAME (DOT NAME)+
    ;
name_or_attr
    : NAME (DOT NAME)*
    ;

group_pattern
    : LPAR pattern RPAR;

sequence_pattern
    : LSQB maybe_sequence_pattern? RSQB
    | LPAR open_sequence_pattern? RPAR;

open_sequence_pattern
    : maybe_star_pattern COMMA maybe_sequence_pattern?;

maybe_sequence_pattern
    : maybe_star_pattern (COMMA maybe_star_pattern)* COMMA?;

maybe_star_pattern
    : star_pattern
    | pattern;

star_pattern
    : STAR NAME;

mapping_pattern
    : LBRACE RBRACE
    | LBRACE double_star_pattern COMMA? RBRACE
    | LBRACE items_pattern (COMMA double_star_pattern)? COMMA? RBRACE
    ;

items_pattern
    : key_value_pattern (COMMA key_value_pattern)*;

key_value_pattern
    : (literal_expr | attr) COLON pattern;

double_star_pattern
    : DOUBLESTAR pattern_capture_target;

class_pattern
    : name_or_attr LPAR ((positional_patterns (COMMA keyword_patterns)? | keyword_patterns) COMMA?)? RPAR
    ;



positional_patterns
    : pattern (COMMA pattern)*;

keyword_patterns
    : keyword_pattern (COMMA keyword_pattern)*;

keyword_pattern
    : NAME EQUAL pattern;

// Type statement
// ---------------

type_alias
    : soft_kw_type NAME type_params? EQUAL expression;

// Type parameter declaration
// --------------------------

type_params: LSQB type_param_seq  RSQB;

type_param_seq: type_param (COMMA type_param)* COMMA?;

type_param
    : NAME type_param_bound?
    | STAR  NAME
    | DOUBLESTAR NAME
    ;


type_param_bound: COLON expression;

// EXPRESSIONS
// -----------

expressions
    : expression (COMMA expression )* COMMA?
    ;


expression
    : disjunction (IF disjunction ELSE expression)?
    | lambdef
    ;

yield_expr
    : YIELD (FROM expression | star_expressions?)
    ;

star_expressions
    : star_expression (COMMA star_expression )* COMMA?
    ;


star_expression
    : STAR bitwise_or
    | expression;

star_named_expressions: star_named_expression (COMMA star_named_expression)* COMMA?;

star_named_expression
    : STAR bitwise_or
    | named_expression;

assignment_expression
    : NAME COLONEQUAL expression;

named_expression
    : assignment_expression
    | expression;

disjunction
    : conjunction (OR conjunction )*
    ;

conjunction
    : inversion (AND inversion )*
    ;

inversion
    : NOT inversion
    | comparison;

// Comparison operators
// --------------------

comparison
    : bitwise_or compare_op_bitwise_or_pair*
    ;

compare_op_bitwise_or_pair
    : eq_bitwise_or
    | noteq_bitwise_or
    | lte_bitwise_or
    | lt_bitwise_or
    | gte_bitwise_or
    | gt_bitwise_or
    | notin_bitwise_or
    | in_bitwise_or
    | isnot_bitwise_or
    | is_bitwise_or;

eq_bitwise_or: EQEQUAL bitwise_or;
noteq_bitwise_or
    : (NOTEQUAL ) bitwise_or;
lte_bitwise_or: LESSEQUAL bitwise_or;
lt_bitwise_or: LESS bitwise_or;
gte_bitwise_or: GREATEREQUAL bitwise_or;
gt_bitwise_or: GREATER bitwise_or;
notin_bitwise_or: NOT IN bitwise_or;
in_bitwise_or: IN bitwise_or;
isnot_bitwise_or: IS NOT bitwise_or;
is_bitwise_or: IS bitwise_or;

// Bitwise operators
// -----------------

bitwise_or
    : bitwise_or VBAR bitwise_xor
    | bitwise_xor;

bitwise_xor
    : bitwise_xor CIRCUMFLEX bitwise_and
    | bitwise_and;

bitwise_and
    : bitwise_and AMPER shift_expr
    | shift_expr;

shift_expr
    : shift_expr (LEFTSHIFT | RIGHTSHIFT) sum
    | sum
    ;

// Arithmetic operators
// --------------------

sum
    : sum (PLUS | MINUS) term
    | term
    ;

term
    : term (STAR | SLASH | DOUBLESLASH | PERCENT | AT) factor
    | factor
    ;




factor
    : PLUS factor
    | MINUS factor
    | TILDE factor
    | power;

power
    : await_primary (DOUBLESTAR factor)?
    ;

// Primary elements
// ----------------

// Primary elements are things like "obj.something.something", "obj[something]", "obj(something)", "obj" ...

await_primary
    : AWAIT primary
    | primary;

primary
    : primary (DOT NAME | genexp | LPAR arguments? RPAR | LSQB slices RSQB)
    | atom
    ;



slices
    : slice
    | (slice | starred_expression) (COMMA (slice | starred_expression))* COMMA?;

slice
    : expression? COLON expression? (COLON expression? )?
    | named_expression;

atom
    : NAME
    | TRUE
    | FALSE
    | NONE
    | strings
    | NUMBER
    | (tuple | group | genexp)
    | (list | listcomp)
    | (dict | set | dictcomp | setcomp)
    | ELLIPSIS;

group
    : LPAR (yield_expr | named_expression) RPAR;

// Lambda functions
// ----------------

lambdef
    : LAMBDA lambda_params? COLON expression;

lambda_params
    : lambda_parameters;

// lambda_parameters etc. duplicates parameters but without annotations
// or type comments, and if there's no comma after a parameter, we expect
// a colon, not a close parenthesis.  (For more, see parameters above.)
//
lambda_parameters
    : lambda_slash_no_default lambda_param_no_default* lambda_param_with_default* lambda_star_etc?
    | lambda_slash_with_default lambda_param_with_default* lambda_star_etc?
    | lambda_param_no_default+ lambda_param_with_default* lambda_star_etc?
    | lambda_param_with_default+ lambda_star_etc?
    | lambda_star_etc;

lambda_slash_no_default
    : lambda_param_no_default+ SLASH COMMA?
    ;

lambda_slash_with_default
    : lambda_param_no_default* lambda_param_with_default+ SLASH COMMA?
    ;

lambda_star_etc
    : STAR lambda_param_no_default lambda_param_maybe_default* lambda_kwds?
    | STAR COMMA lambda_param_maybe_default+ lambda_kwds?
    | lambda_kwds;

lambda_kwds
    : DOUBLESTAR lambda_param_no_default;

lambda_param_no_default
    : lambda_param COMMA?
    ;
lambda_param_with_default
    : lambda_param default_assignment COMMA?
    ;
lambda_param_maybe_default
    : lambda_param default_assignment? COMMA?
    ;
lambda_param: NAME;

// LITERALS
// ========

fstring_middle
    : fstring_replacement_field
    | FSTRING_MIDDLE;
fstring_replacement_field
    : LBRACE (yield_expr | star_expressions) EQUAL? fstring_conversion? fstring_full_format_spec? RBRACE;
fstring_conversion
    : EXCLAMATION NAME;
fstring_full_format_spec
    : COLON fstring_format_spec*;
fstring_format_spec
    : FSTRING_MIDDLE
    | fstring_replacement_field;
fstring
    : FSTRING_START fstring_middle* FSTRING_END;

string: STRING;
strings: (fstring|string)+;

list
    : LSQB star_named_expressions? RSQB;

tuple
    : LPAR (star_named_expression COMMA star_named_expressions?  )? RPAR;

set: LBRACE star_named_expressions RBRACE;

// Dicts
// -----

dict
    : LBRACE double_starred_kvpairs? RBRACE;

double_starred_kvpairs: double_starred_kvpair (COMMA double_starred_kvpair)* COMMA?;

double_starred_kvpair
    : DOUBLESTAR bitwise_or
    | kvpair;

kvpair: expression COLON expression;

// Comprehensions & Generators
// ---------------------------

for_if_clauses
    : for_if_clause+;

for_if_clause
    : ASYNC? FOR star_targets IN disjunction (IF disjunction )*
    ;

listcomp
    : LSQB named_expression for_if_clauses RSQB;

setcomp
    : LBRACE named_expression for_if_clauses RBRACE;

genexp
    : LPAR ( assignment_expression | expression) for_if_clauses RPAR;

dictcomp
    : LBRACE kvpair for_if_clauses RBRACE;

// FUNCTION CALL ARGUMENTS
// =======================

arguments
    : args COMMA?;

args
    : (starred_expression | ( assignment_expression | expression)) (COMMA (starred_expression | ( assignment_expression | expression)))* (COMMA kwargs )?
    | kwargs;

kwargs
    : kwarg_or_starred (COMMA kwarg_or_starred)* (COMMA kwarg_or_double_starred (COMMA kwarg_or_double_starred)*)?
    | kwarg_or_double_starred (COMMA kwarg_or_double_starred)*
    ;

starred_expression
    : STAR expression;

kwarg_or_starred
    : NAME EQUAL expression
    | starred_expression;

kwarg_or_double_starred
    : NAME EQUAL expression
    | DOUBLESTAR expression;

// ASSIGNMENT TARGETS
// ==================

// Generic targets
// ---------------

// NOTE: star_targets may contain *bitwise_or, targets may not.
star_targets
    : star_target (COMMA star_target )* COMMA?
    ;

star_targets_list_seq: star_target (COMMA star_target)+ COMMA?;

star_targets_tuple_seq
    : star_target (COMMA | (COMMA star_target )+ COMMA?)
    ;

star_target
    : STAR (star_target)
    | target_with_star_atom;

target_with_star_atom
    : t_primary (DOT NAME | LSQB slices RSQB)
    | star_atom
    ;

star_atom
    : NAME
    | LPAR target_with_star_atom RPAR
    | LPAR star_targets_tuple_seq? RPAR
    | LSQB star_targets_list_seq? RSQB;

single_target
    : single_subscript_attribute_target
    | NAME
    | LPAR single_target RPAR;

single_subscript_attribute_target
    : t_primary (DOT NAME | LSQB slices RSQB)
    ;

t_primary
    : t_primary (DOT NAME | LSQB slices RSQB | genexp | LPAR arguments? RPAR)
    | atom
    ;





// Targets for del statements
// --------------------------

del_targets: del_target (COMMA del_target)* COMMA?;

del_target
    : t_primary (DOT NAME | LSQB slices RSQB)
    | del_t_atom
    ;

del_t_atom
    : NAME
    | LPAR del_target RPAR
    | LPAR del_targets? RPAR
    | LSQB del_targets? RSQB;

// TYPING ELEMENTS
// ---------------


// type_expressions allow */** but ignore them
type_expressions
    : expression (COMMA expression)* (COMMA (STAR expression (COMMA DOUBLESTAR expression)? | DOUBLESTAR expression))?
    | STAR expression (COMMA DOUBLESTAR expression)?
    | DOUBLESTAR expression
    ;



func_type_comment
    : NEWLINE TYPE_COMMENT   // Must be followed by indented block
    | TYPE_COMMENT;

// *** Soft Keywords:  https://docs.python.org/3.12/reference/lexical_analysis.html#soft-keywords
soft_kw_type:           {this.isEqualToCurrentTokenText("type")}?  NAME;
soft_kw_match:          {this.isEqualToCurrentTokenText("match")}? NAME;
soft_kw_case:           {this.isEqualToCurrentTokenText("case")}?  NAME;
soft_kw_wildcard:       {this.isEqualToCurrentTokenText("_")}?     NAME;
soft_kw__not__wildcard: {this.isnotEqualToCurrentTokenText("_")}?  NAME;

// ========================= END OF THE GRAMMAR ===========================