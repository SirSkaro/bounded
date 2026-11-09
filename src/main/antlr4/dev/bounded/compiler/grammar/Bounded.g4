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
    | USECASE
    ;

archeOperation
    : archeOperationKind IDENTIFIER SEMICOLON
    ;

archeOperationKind
    : FUNCTION
    | QUERY
    | COMMAND
    | ACTION
    ;

VALUE      : 'value';
ENTITY     : 'entity';
USECASE    : 'usecase';
FUNCTION   : 'function';
QUERY      : 'query';
COMMAND    : 'command';
ACTION     : 'action';
LBRACE     : '{';
RBRACE     : '}';
SEMICOLON  : ';';
IDENTIFIER : [a-zA-Z_] [a-zA-Z0-9_]*;
WHITESPACE : [ \t\r\n]+ -> skip;
