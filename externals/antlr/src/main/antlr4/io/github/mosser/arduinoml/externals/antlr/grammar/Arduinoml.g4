grammar Arduinoml;


/******************
 ** Parser rules **
 ******************/

root            :   declaration bricks states EOF;

declaration     :   'application' name=IDENTIFIER;

bricks          :   (sensor|actuator)+;
    sensor      :   'sensor'   location ;
    actuator    :   'actuator' location ;
    location    :   id=IDENTIFIER ':' type=TYPE;

states          :   state+;
    state       :   initial? name=IDENTIFIER '{'  action+ transition '}';
    action      :   receiver=IDENTIFIER '<=' value=SIGNAL;
    transition  :   condition '=>' next=IDENTIFIER ;

    sensorCondition :   receiver=IDENTIFIER 'is' value=SIGNAL;
    unaryCondition :   operator=UNARYOPERATOR condition;
    binaryCondition : '(' left=condition operator=BINARYOPERATOR right=condition ')';

    condition : (sensorCondition | unaryCondition | binaryCondition);

    initial     :   '->';

/*****************
 ** Lexer rules **
 *****************/

IDENTIFIER      :   LOWERCASE (LOWERCASE|UPPERCASE)+;
SIGNAL          :   'HIGH' | 'LOW';
BINARYOPERATOR  :   'AND' | 'OR';
TYPE            :   'ANALOG' | 'DIGITAL';
UNARYOPERATOR   :   'NOT';

/*************
 ** Helpers **
 *************/

fragment LOWERCASE  : [a-z];                                 // abstract rule, does not really exists
fragment UPPERCASE  : [A-Z];
NEWLINE             : ('\r'? '\n' | '\r')+      -> skip;
WS                  : ((' ' | '\t')+)           -> skip;     // who cares about whitespaces?
COMMENT             : '#' ~( '\r' | '\n' )*     -> skip;     // Single line comments, starting with a #
