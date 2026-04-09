grammar FlinkPipeline;

// ─────────────────────────────────────────────
//  Top-level
// ─────────────────────────────────────────────

program
    : pipeline+ EOF
    ;

pipeline
    : PIPELINE identifier LBRACE
        parallelismStmt
        sourceStmt
        transformStmt+
        sinkStmt
      RBRACE
    ;

// ─────────────────────────────────────────────
//  PARALLELISM — required Flink operator config
// ─────────────────────────────────────────────

parallelismStmt
    : PARALLELISM INTEGER
    ;

// ─────────────────────────────────────────────
//  SOURCE
// ─────────────────────────────────────────────

sourceStmt
    : SOURCE identifier FROM connectorExpr schemaBlock
    ;

schemaBlock
    : SCHEMA LBRACE fieldDecl+ RBRACE
    ;

fieldDecl
    : identifier COLON schemaType
    ;

schemaType
    : T_STRING
    | T_INT
    | T_LONG
    | T_DOUBLE
    | T_FLOAT
    | T_BOOLEAN
    ;

connectorExpr
    : KAFKA LPAREN stringLiteral RPAREN       # kafkaConnector
    | FILE  LPAREN stringLiteral RPAREN       # fileConnector
    ;

// ─────────────────────────────────────────────
//  TRANSFORMS — map, flatMap, filter
// ─────────────────────────────────────────────

transformStmt
    : FILTER expression                       # filterTransform
    | MAP    LBRACE fieldAssignment+ RBRACE   # mapTransform
    | FLATMAP LBRACE fieldAssignment+ RBRACE  # flatMapTransform
    ;

fieldAssignment
    : identifier ASSIGN expression
    ;

// ─────────────────────────────────────────────
//  SINK
// ─────────────────────────────────────────────

sinkStmt
    : SINK TO connectorExpr FORMAT formatType
    ;

formatType
    : JSON
    | CSV
    ;

// ─────────────────────────────────────────────
//  EXPRESSIONS
// ─────────────────────────────────────────────

// Alternatives are ordered highest-to-lowest precedence (ANTLR 4: first = highest).
// Non-left-recursive alternatives (NOT, paren, atoms) are unaffected by ordering.
expression
    : NOT expression                          # notExpr
    | expression mulOp expression             # mulExpr
    | expression addOp expression             # addExpr
    | expression compOp expression            # compareExpr
    | expression OR  expression               # orExpr
    | expression AND expression               # andExpr
    | LPAREN expression RPAREN                # parenExpr
    | fieldAccess                             # fieldExpr
    | literal                                 # literalExpr
    ;

fieldAccess
    : identifier (DOT identifier)*
    ;

compOp : EQ | NEQ | LT | GT | LTE | GTE ;
addOp  : PLUS | MINUS ;
mulOp  : STAR | SLASH ;

literal
    : INTEGER                                 # intLiteral
    | FLOAT                                   # floatLiteral
    | stringLiteral                           # strLiteral
    | BOOLEAN                                 # boolLiteral
    ;

stringLiteral : STRING ;
identifier    : ID ;

// ─────────────────────────────────────────────
//  KEYWORDS
// ─────────────────────────────────────────────

PIPELINE    : 'pipeline' ;
PARALLELISM : 'parallelism' ;
SOURCE      : 'source' ;
FROM        : 'from' ;
SCHEMA      : 'schema' ;
FILTER      : 'filter' ;
MAP         : 'map' ;
FLATMAP     : 'flatMap' ;
SINK        : 'sink' ;
TO          : 'to' ;
FORMAT      : 'format' ;
KAFKA       : 'kafka' ;
FILE        : 'file' ;
JSON        : 'json' ;
CSV         : 'csv' ;
AND         : 'and' ;
OR          : 'or' ;
NOT         : 'not' ;

// ─────────────────────────────────────────────
//  SCHEMA TYPES
// ─────────────────────────────────────────────

T_STRING  : 'string' ;
T_INT     : 'int' ;
T_LONG    : 'long' ;
T_DOUBLE  : 'double' ;
T_FLOAT   : 'float' ;
T_BOOLEAN : 'boolean' ;

// ─────────────────────────────────────────────
//  OPERATORS & PUNCTUATION
// ─────────────────────────────────────────────

COLON  : ':' ;
LBRACE : '{' ;
RBRACE : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
DOT    : '.' ;
ASSIGN : '=' ;
EQ     : '==' ;
NEQ    : '!=' ;
LT     : '<' ;
GT     : '>' ;
LTE    : '<=' ;
GTE    : '>=' ;
PLUS   : '+' ;
MINUS  : '-' ;
STAR   : '*' ;
SLASH  : '/' ;

// ─────────────────────────────────────────────
//  LITERALS & IDENTIFIERS
// ─────────────────────────────────────────────

BOOLEAN : 'true' | 'false' ;
INTEGER : [0-9]+ ;
FLOAT   : [0-9]+ '.' [0-9]+ ;
STRING  : '"' (~["\r\n] | '\\"')* '"' ;
ID      : [a-zA-Z_][a-zA-Z_0-9]* ;

// ─────────────────────────────────────────────
//  WHITESPACE & COMMENTS
// ─────────────────────────────────────────────

WS            : [ \t\r\n]+    -> skip ;
COMMENT       : '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT : '/*' .*? '*/' -> skip ;