grammar SysY;

program
   : compUnit
   ;

compUnit
   : (funcDef | decl)+
   ;

decl
   : constDecl
   | varDecl
   ;

constDecl
   : CONST_KW bType constDef (COMMA constDef)* SEMICOLON
   ;

bType
   : INT_KW
   ;

constDef
   : IDENT (L_BRACKT constExp R_BRACKT)* ASSIGN constInitVal
   ;

constInitVal
   : constExp
   | (L_BRACE (constInitVal (COMMA constInitVal)*)? R_BRACE)
   ;

varDecl
   : bType varDef (COMMA varDef)* SEMICOLON
   ;

varDef
   : IDENT (L_BRACKT constExp R_BRACKT)* (ASSIGN initVal)?
   ;

initVal
   : exp
   | (L_BRACE (initVal (COMMA initVal)*)? R_BRACE)
   ;

funcDef
   : funcType IDENT L_PAREN funcFParams? R_PAREN block
   ;

funcType
   : VOID_KW
   | INT_KW
   ;

funcFParams
   : funcFParam (COMMA funcFParam)*
   ;

funcFParam
   : bType IDENT (L_BRACKT R_BRACKT (L_BRACKT exp R_BRACKT)*)?
   ;

block
   : L_BRACE blockItem* R_BRACE
   ;

blockItem
   : constDecl
   | varDecl
   | stmt
   ;

stmt
   : assignStmt
   | expStmt
   | block
   | conditionStmt
   | whileStmt
   | breakStmt
   | continueStmt
   | returnStmt
   ;

assignStmt
   : lVal ASSIGN exp SEMICOLON
   ;

expStmt
   : exp? SEMICOLON
   ;

conditionStmt
   : IF_KW L_PAREN cond R_PAREN stmt (ELSE_KW stmt)?
   ;

whileStmt
   : WHILE_KW L_PAREN cond R_PAREN stmt
   ;

breakStmt
   : BREAK_KW SEMICOLON
   ;

continueStmt
   : CONTINUE_KW SEMICOLON
   ;

returnStmt
   : RETURN_KW (exp)? SEMICOLON
   ;

exp
   : addExp
   ;

cond
   : lOrExp
   ;

lVal
   : IDENT (L_BRACKT exp R_BRACKT)*
   ;

primaryExp
   : (L_PAREN exp R_PAREN)
   | lVal
   | number
   ;

number
   : intConst
   ;

intConst
   : DECIMAL_CONST
   | OCTAL_CONST
   | HEXADECIMAL_CONST
   ;

unaryExp
   : primaryExp
   | callee
   | (unaryOp unaryExp)
   ;

callee
   : IDENT L_PAREN funcRParams? R_PAREN
   ;

unaryOp
   : PLUS
   | MINUS
   | NOT
   ;

funcRParams
   : param (COMMA param)*
   ;

param
   : exp
   | STRING
   ;

mulExp
   : unaryExp (mulOp unaryExp)*
   ; // eliminate left-recursive
   
mulOp
   : MUL
   | DIV
   | MOD
   ;

addExp
   : mulExp (addOp mulExp)*
   ; // eliminate left-recursive
   
addOp
   : PLUS
   | MINUS
   ;

relExp
   : addExp (relOp addExp)*
   ; // eliminate left-recursive
   
relOp
   : LT
   | GT
   | LE
   | GE
   ;

eqExp
   : relExp (eqOp relExp)*
   ;

eqOp
   : EQ
   | NEQ
   ;

lAndExp
   : eqExp (AND eqExp)*
   ;

lOrExp
   : lAndExp (OR lAndExp)*
   ;

constExp
   : addExp
   ;

CONST_KW
   : 'const'
   ;

INT_KW
   : 'int'
   ;

VOID_KW
   : 'void'
   ;

IF_KW
   : 'if'
   ;

ELSE_KW
   : 'else'
   ;

WHILE_KW
   : 'while'
   ;

BREAK_KW
   : 'break'
   ;

CONTINUE_KW
   : 'continue'
   ;

RETURN_KW
   : 'return'
   ;

IDENT
   : [_a-zA-Z]
   | [_a-zA-Z] [_a-zA-Z0-9]+
   ;

DECIMAL_CONST
   : [1-9]
   | [1-9] [0-9]+
   ;

OCTAL_CONST
   : '0'
   | ('0' [0-7]+)
   ;

HEXADECIMAL_CONST
   : ('0x' | '0X') [a-fA-F0-9]+
   ;

STRING
   : DOUBLE_QUOTE REGULAR_CHAR* DOUBLE_QUOTE
   ;

fragment REGULAR_CHAR
   : ~ ["\\]
   ;

PLUS
   : '+'
   ;

MINUS
   : '-'
   ;

NOT
   : '!'
   ;

MUL
   : '*'
   ;

DIV
   : '/'
   ;

MOD
   : '%'
   ;

ASSIGN
   : '='
   ;

EQ
   : '=='
   ;

NEQ
   : '!='
   ;

LT
   : '<'
   ;

GT
   : '>'
   ;

LE
   : '<='
   ;

GE
   : '>='
   ;

AND
   : '&&'
   ;

OR
   : '||'
   ;

L_PAREN
   : '('
   ;

R_PAREN
   : ')'
   ;

L_BRACE
   : '{'
   ;

R_BRACE
   : '}'
   ;

L_BRACKT
   : '['
   ;

R_BRACKT
   : ']'
   ;

COMMA
   : ','
   ;

SEMICOLON
   : ';'
   ;

DOUBLE_QUOTE
   : '"'
   ;

WS
   : [ \r\n\t]+ -> skip
   ;

LINE_COMMENT
   : '//' ~ [\r\n]* -> skip
   ;

MULTILINE_COMMENT
   : '/*' .*? '*/' -> skip
   ;

