/* FM2ExConf - A Generator for Excel-based configurators
 * Copyright (C) 2020-2020  AIG team, Institute for Software Technology,
 * Graz University of Technology, Austria
 *
 * FM2ExConf is a tool enabling translate feature models into
 * configurators in Excel worksheet.
 *
 * Contact: http://ase.ist.tugraz.at/ASE/
 * Author: Viet-Man Le (vietman.le@ist.tugraz.at)
 */

lexer grammar CommonLexer;

@lexer::header {
}

/*********************************************
 * KEYWORDS
 **********************************************/

FM4CONFversion : 'FM4Conf-v1.0';

MODELNAME : 'MODEL';
FEATURE : 'FEATURES';
RELATIONSHIP : 'RELATIONSHIPS';
CONSTRAINT : 'CONSTRAINTS';

MANDATORY : 'mandatory';
OPTIONAL : 'optional';
ALTERNATIVE : 'alternative';
OR : 'or';
REQUIRES : 'requires';
EXCLUDES : 'excludes';

//DD:'..';
//DO:'.';
CM:',';

//PL:'+';
//MN:'-';
SC:';';
CL:':';
//DC:'::';
LP:'(';
RP:')';

/*********************************************
 * GENERAL
 **********************************************/

NAME : ID ( SPACE ID )* ;

COMMENT
    :   '%' ~('\n'|'\r')* '\n' -> skip
    ;

WS  :   ( ' '
        | '\t'
        | '\r'
        | '\n'
        ) -> skip
    ; // toss out whitespace

/*********************************************
 * FRAGMENTS
 **********************************************/

fragment ID : ID_HEAD ID_TAIL* ;
fragment ID_HEAD : LETTER ;
fragment ID_TAIL : LETTER | DIGIT;
fragment LETTER : [a-zA-Z_-] ;
fragment DIGIT : [0-9] ;
fragment SPACE : ' '+ ;