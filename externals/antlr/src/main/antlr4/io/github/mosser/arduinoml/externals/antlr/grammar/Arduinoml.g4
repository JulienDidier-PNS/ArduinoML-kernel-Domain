grammar Arduinoml;


/******************
 ** Parser rules **
 ******************/

root            :   declaration bricks states EOF;

declaration     :   'application' name=IDENTIFIER;

bricks          :   (sensor|actuator)+;
    sensor      :   'sensor'   location ;
    actuator    :   'actuator' location ;
    location    :   id=IDENTIFIER;

states          :   state+;
    state       :   initial? name=IDENTIFIER '{'  action+ transition '}';
    action      :   receiver=IDENTIFIER '<=' value=SIGNAL;
    transition  :   conditions '=>' next=IDENTIFIER ;
    conditions  :   condition ( OPERATOR condition )*;
    condition   :   receiver=IDENTIFIER 'is' value=SIGNAL;
    initial     :   '->';

/*****************
 ** Lexer rules **
 *****************/

IDENTIFIER      :   LOWERCASE (LOWERCASE|UPPERCASE)+;
SIGNAL          :   'HIGH' | 'LOW';
OPERATOR        :   'AND' | 'LOW';

/*************
 ** Helpers **
 *************/

fragment LOWERCASE  : [a-z];                                 // abstract rule, does not really exists
fragment UPPERCASE  : [A-Z];
NEWLINE             : ('\r'? '\n' | '\r')+      -> skip;
WS                  : ((' ' | '\t')+)           -> skip;     // who cares about whitespaces?
COMMENT             : '#' ~( '\r' | '\n' )*     -> skip;     // Single line comments, starting with a #
