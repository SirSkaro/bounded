grammar Bounded;


program
    : archeClass EOF
    ;

archeClass
    : archeClassKind IDENTIFIER LBRACE archeOperation* RBRACE
    ;

archeClassKind
    : VALUE
    | ENTITY
    ;

archeOperation
    : archeOperationKind IDENTIFIER SEMICOLON
    ;

archeOperationKind
    : FUNCTION
    | QUERY
    | COMMAND
    ;

VALUE      : 'value';
ENTITY     : 'entity';
FUNCTION   : 'function';
QUERY      : 'query';
COMMAND    : 'command';
LBRACE     : '{';
RBRACE     : '}';
SEMICOLON  : ';';
IDENTIFIER : [a-zA-Z_] [a-zA-Z0-9_]*;
WHITESPACE : [ \t\r\n]+ -> skip;
