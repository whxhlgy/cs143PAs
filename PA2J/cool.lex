/*
 *  The scanner definition for COOL.
 */

import java_cup.runtime.Symbol;

%%

%{

/*  Stuff enclosed in %{ %} is copied verbatim to the lexer class
 *  definition, all the extra variables/functions you want to use in the
 *  lexer actions should go here.  Don't remove or modify anything that
 *  was there initially.  */

    // Max size of string constants
    static int MAX_STR_CONST = 1025;

    // For assembling string constants
    StringBuffer string_buf = new StringBuffer();
    private int comment_level = 0;

    private int curr_lineno = 1;
    int get_curr_lineno() {
	return curr_lineno;
    }

    private AbstractSymbol filename;

    void set_filename(String fname) {
	filename = AbstractTable.stringtable.addString(fname);
    }

    AbstractSymbol curr_filename() {
	return filename;
    }
%}

%init{

/*  Stuff enclosed in %init{ %init} is copied verbatim to the lexer
 *  class constructor, all the extra initialization you want to do should
 *  go here.  Don't remove or modify anything that was there initially. */

    // empty for now
%init}

%eofval{

/*  Stuff enclosed in %eofval{ %eofval} specifies java code that is
 *  executed when end-of-file is reached.  If you use multiple lexical
 *  states and want to do something special if an EOF is encountered in
 *  one of those states, place your code in the switch statement.
 *  Ultimately, you should return the EOF symbol, or your lexer won't
 *  work.  */

    switch(yy_lexical_state) {
    case YYINITIAL:
	/* nothing special to do in the initial state */
      break;
	   case COMMENT:
     yybegin(YYINITIAL);
	   return new Symbol(TokenConstants.ERROR, "EOF in comment");
     case STRING:
     yybegin(YYINITIAL);
     return new Symbol(TokenConstants.ERROR, "EOF in string constant");
    }
    return new Symbol(TokenConstants.EOF);
%eofval}

%class CoolLexer
%cup

TypeId = [A-Z][A-Za-z0-9_]*
ObjectId = [a-z][A-Za-z0-9_]*
Integers = [0-9]+

a = [aA]
b = [bB]
c = [cC]
d = [dD]
e = [eE]
f = [fF]
g = [gG]
h = [hH]
i = [iI]
j = [jJ]
k = [kK]
l = [lL]
m = [mM]
n = [nN]
o = [oO]
p = [pP]
q = [qQ]
r = [rR]
s = [sS]
t = [tT]
u = [uU]
v = [vV]
w = [wW]
x = [xX]
y = [yY]
z = [zZ]

class = {c}{l}{a}{s}{s}
else = {e}{l}{s}{e}
fi = {f}{i}
if = {i}{f}
in = {i}{n}
inherits = {i}{n}{h}{e}{r}{i}{t}{s}
isvoid = {i}{s}{v}{o}{i}{d}
let = {l}{e}{t}
loop = {l}{o}{o}{p}
pool = {p}{o}{o}{l}
then = {t}{h}{e}{n}
while = {w}{h}{i}{l}{e}
case = {c}{a}{s}{e}
esac = {e}{s}{a}{c}
new = {n}{e}{w}
of = {o}{f}
not = {n}{o}{t}
true = t{r}{u}{e}
false = f{a}{l}{s}{e}
invalid_char = ['\[\]>_\!#$%^&?`\\\|\001\002\003\004\000]
whitespace = [ \t\n\r\f\v]

%state COMMENT
%state STRING

%%

<YYINITIAL>\n    { curr_lineno++; }
<YYINITIAL>{invalid_char} {
  return new Symbol(TokenConstants.ERROR, yytext());
}

<YYINITIAL>\+    { return new Symbol(TokenConstants.PLUS); }
<YYINITIAL>/     { return new Symbol(TokenConstants.DIV); }
<YYINITIAL>-     { return new Symbol(TokenConstants.MINUS); }
<YYINITIAL>\*    { return new Symbol(TokenConstants.MULT); }
<YYINITIAL>=     { return new Symbol(TokenConstants.EQ); }
<YYINITIAL><     { return new Symbol(TokenConstants.LT); }
<YYINITIAL>\.    { return new Symbol(TokenConstants.DOT); }
<YYINITIAL>~     { return new Symbol(TokenConstants.NEG); }
<YYINITIAL>,     { return new Symbol(TokenConstants.COMMA); }
<YYINITIAL>;     { return new Symbol(TokenConstants.SEMI); }
<YYINITIAL>:     { return new Symbol(TokenConstants.COLON); }
<YYINITIAL>\(    { return new Symbol(TokenConstants.LPAREN); }
<YYINITIAL>\)    { return new Symbol(TokenConstants.RPAREN); }
<YYINITIAL>@     { return new Symbol(TokenConstants.AT); }
<YYINITIAL>\{    { return new Symbol(TokenConstants.LBRACE); }
<YYINITIAL>\}    { return new Symbol(TokenConstants.RBRACE); }
<YYINITIAL><-   { return new Symbol(TokenConstants.ASSIGN); }

<YYINITIAL>{class}  { return new Symbol(TokenConstants.CLASS); }
<YYINITIAL>{else}   { return new Symbol(TokenConstants.ELSE); }
<YYINITIAL>{fi}     { return new Symbol(TokenConstants.FI); }
<YYINITIAL>{if}     { return new Symbol(TokenConstants.IF); }
<YYINITIAL>{in}     { return new Symbol(TokenConstants.IN); }
<YYINITIAL>{inherits} { return new Symbol(TokenConstants.INHERITS); }
<YYINITIAL>{isvoid} { return new Symbol(TokenConstants.ISVOID); }
<YYINITIAL>{let}    { return new Symbol(TokenConstants.LET); }
<YYINITIAL>{loop}   { return new Symbol(TokenConstants.LOOP); }
<YYINITIAL>{pool}   { return new Symbol(TokenConstants.POOL); }
<YYINITIAL>{then}   { return new Symbol(TokenConstants.THEN); }
<YYINITIAL>{while}  { return new Symbol(TokenConstants.WHILE); }
<YYINITIAL>{case}   { return new Symbol(TokenConstants.CASE); }
<YYINITIAL>{esac}   { return new Symbol(TokenConstants.ESAC); }
<YYINITIAL>{new}    { return new Symbol(TokenConstants.NEW); }
<YYINITIAL>{of}     { return new Symbol(TokenConstants.OF); }
<YYINITIAL>{not}    { return new Symbol(TokenConstants.NOT); }
<YYINITIAL>{true}   { return new Symbol(TokenConstants.BOOL_CONST, Boolean.parseBoolean(yytext())); }
<YYINITIAL>{false}   { return new Symbol(TokenConstants.BOOL_CONST, Boolean.parseBoolean(yytext())); }

<YYINITIAL>"<="   { return new Symbol(TokenConstants.LE); }
<YYINITIAL>"=>"		{ return new Symbol(TokenConstants.DARROW); }
<YYINITIAL>"--".*   { }
<YYINITIAL>"(*"    { 
  yybegin(COMMENT);
  comment_level = 1;
}
<YYINITIAL>"*)"    { return new Symbol(TokenConstants.ERROR, "Unmatched *)"); }
<YYINITIAL>[ \t\r]+       { /* do nothing */}
<YYINITIAL>\"       { 
  yybegin(STRING); 
  string_buf.setLength(0);
}
<YYINITIAL>{TypeId} { 
  return new Symbol(TokenConstants.TYPEID, AbstractTable.idtable.addString(yytext())); 
}
<YYINITIAL>{ObjectId} { 
  return new Symbol(TokenConstants.OBJECTID, AbstractTable.idtable.addString(yytext())); 
}
<YYINITIAL>{Integers} { 
  return new Symbol(TokenConstants.INT_CONST, AbstractTable.inttable.addString(yytext())); 
}
<YYINITIAL>{whitespace} {

}


<COMMENT>\n  { curr_lineno++; }
<COMMENT>\\\n { curr_lineno++; }
<COMMENT>\\[^] { /* escape anything */ }
<COMMENT>"(*"		{ comment_level++; }
<COMMENT>"*)" 		{ 
  if (--comment_level == 0) {
    yybegin(YYINITIAL);
  }
}
<COMMENT> [^] 		    {}


<STRING>[^\0\\\n\"]* 		{
	string_buf.append(yytext());
  //System.out.println("append text: " + yytext());
}
<STRING>\\\n    {
  string_buf.append("\n");
  //System.out.println("append newline");
}
<STRING>\\\0.*\" {
  yybegin(YYINITIAL);
  return new Symbol(TokenConstants.ERROR, "String contains escaped null character");
}
<STRING>\0.*\" {
  yybegin(YYINITIAL);
  return new Symbol(TokenConstants.ERROR, "String contains null character");
}
<STRING>\\. {
  String text = yytext();
  switch (text.charAt(1)) {
  case 'n':
    string_buf.append("\n");
    break;
  case 't':
    string_buf.append("\t");
    break;
  case 'b':
    string_buf.append("\b");
    break;
  case 'f':
    string_buf.append("\f");
    break;
  default:
    string_buf.append(text.charAt(1));
    break;
  }
}
<STRING>\n {
  yybegin(YYINITIAL);
  return new Symbol(TokenConstants.ERROR, "Unterminated string constant");
  //System.out.println("ERROR");
}
<STRING>\"       {
  yybegin(YYINITIAL);
  if (string_buf.length() >= MAX_STR_CONST) {
    return new Symbol(TokenConstants.ERROR, "String constant too long");
  }
  String str = string_buf.toString();
  return new Symbol(TokenConstants.STR_CONST, AbstractTable.stringtable.addString(str));
}

\n { }
[^] {}
. { System.err.println("LEXER BUG - UNMATCHED: " + yytext()); }
