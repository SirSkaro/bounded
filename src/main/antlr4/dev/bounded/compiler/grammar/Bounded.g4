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
    | MESSAGE
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
MESSAGE    : 'message';
FUNCTION   : 'function';
QUERY      : 'query';
COMMAND    : 'command';
ACTION     : 'action';
LBRACE     : '{';
RBRACE     : '}';
SEMICOLON  : ';';
IDENTIFIER : [a-zA-Z_] [a-zA-Z0-9_]*;
WHITESPACE : [ \t\r\n]+ -> skip;
