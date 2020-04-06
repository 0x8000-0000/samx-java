/*
   Copyright 2020 Florin Iucha

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

grammar SamX;

@header {
package net.signbit.samx.parser;
}

tokens { INDENT, DEDENT, END, INVALID }

@lexer::members 
{

   private java.util.ArrayDeque<Token> tokens = new java.util.ArrayDeque<Token>();
   private java.util.Stack<Integer> indents = new java.util.Stack<>();

   private Token lastToken;

   @Override
   public void emit(Token t)
   {
      tokens.add(t);
   }

   @Override
   public Token nextToken()
   {
      Token token = super.nextToken();

      if (! tokens.isEmpty())
      {
         token = tokens.remove();
      }

      lastToken = token;
      return lastToken;
   }

   private boolean atStartOfInput() {
      return super.getCharPositionInLine() == 0 && super.getLine() == 1;
   }

   private CommonToken makeToken(int type, String text)
   {
      final int start = this.getCharIndex() - 1;
      CommonToken token = new CommonToken(this._tokenFactorySourcePair, type, DEFAULT_TOKEN_CHANNEL, start, start);
      token.setText(text);
      return token;
   }

   private void addNewLine()
   {
      CommonToken newLine = makeToken(SamXParser.NEWLINE, "\n");
      newLine.setLine(_tokenStartLine);
      newLine.setCharPositionInLine(_tokenStartCharPositionInLine);

      tokens.add(newLine);
   }

   private void addEndBlock()
   {
      CommonToken endBlock = makeToken(SamXParser.END, "¶");
      endBlock.setLine(_tokenStartLine);
      endBlock.setCharPositionInLine(_tokenStartCharPositionInLine);

      tokens.add(endBlock);
   }

   private void addIndent()
   {
      CommonToken indent = makeToken(SamXParser.INDENT, ">>>");

      tokens.add(indent);
   }

   private void addInvalid()
   {
      CommonToken invalid = makeToken(SamXParser.INVALID, "???");

      tokens.add(invalid);
   }

   private void addDedent()
   {
      CommonToken dedent = makeToken(SamXParser.DEDENT, "<<<");
      dedent.setLine(_tokenStartLine);
      dedent.setCharPositionInLine(_tokenStartCharPositionInLine);

      tokens.add(dedent);
   }

   private void popIndents(int level)
   {
      while ((! indents.isEmpty()) && (indents.peek() > level))
      {
         addDedent();
         indents.pop();
      }

      if (indents.isEmpty() || (indents.peek() == level))
      {
         // got back to previous level
      }
      else
      {
         // invalid indent; throw exception
         addInvalid();
      }
   }

}

SKIP_
 : ( SPACES | COMMENT ) -> skip
 ;

fragment SPACES
 : [ \t]+
 ;

fragment COMMENT
 : '#' ~[\r\n\f]*
 ;

NEWLINE
 : ( {atStartOfInput()}?   SPACES
   | ( '\r'? '\n' | '\r' | '\f' ) SPACES?
   )
   {
      final char[] tokenText = getText().toCharArray();
      int thisIndent = 0;
      for (char ch: tokenText)
      {
         if (ch == ' ')
         {
            thisIndent ++;
         }
      }

      final int next = _input.LA(1);
      if (next == '\n')
      {
         // this is an empty line, ignore
         return;
      }

      if (next == EOF)
      {
         // add an extra new line at the end of the file, to close out any pending paragraphs
         addNewLine();
      }

      final int currentIndent = indents.isEmpty() ? 0 : indents.peek();

      if (thisIndent == currentIndent)
      {
         // nothing to do
      }
      else if (thisIndent > currentIndent)
      {
         indents.push(thisIndent);

         addNewLine();
         addIndent();
         skip();
      }
      else
      {
         addNewLine();
         popIndents(thisIndent);
         skip();
      }
   } ;

NAME : [-a-zA-Z_] [-a-zA-Z0-9_.]+ ;

INTEGER : [1-9] [0-9]+ ;

SCHEME : 'http' 's'? ':' ;

keyValuePair: key=NAME '=' value=NAME ;

path : ('/' NAME) * ;

url : SCHEME '//' (authority=NAME '@')? host=NAME (':' port=INTEGER)? path ('?' keyValuePair ('&' keyValuePair)* )? ('#' frag=NAME)? ;

TOKEN : [-a-zA-Z0-9_'.;,]+ ;

TYPESEP : ':' ;

RECSEP : '::' ;

COLSEP : '|' ;

BULLET : '*' ;

HASH : '#' ;

OPEN_PHR : '{' ;

CLOSE_PHR : '}' ;

ESCAPE : '\\' ;

STRING :
   '\'' ( '\\' . | ~[\\\r\n\f'] )* '\''
   | '"' ( '\\' . | ~[\\\r\n\f"] )* '"'
   ;

escapeSeq : ESCAPE . ;

text : (NAME | TOKEN | INTEGER | STRING | '/' | escapeSeq ) + ;

annotation : '(' text ')' ;

attribute :
   '(' '?' text ')'           # ConditionAttr
   | '(' '#' NAME ')'         # NameAttr
   | '(' '*' NAME ')'         # IdAttr
   | '(' '!' NAME ')'         # LanguageAttr
   | '[' text ']'             # Citation
   | '[*' blockName=NAME '/' idName=NAME ']'   # Reference
   ;

phrase : OPEN_PHR text CLOSE_PHR annotation* attribute* ;

localInsert : '>($' text ')' ;

APOSTR : '`' ;

inlineCode : APOSTR text APOSTR ;

flow : ( text | phrase | localInsert | url | inlineCode )+ ;

paragraph : ( flow NEWLINE )+ NEWLINE ;
recordRow : ( COLSEP flow )+ NEWLINE ;

block :
     NAME TYPESEP attribute* description=flow? NEWLINE+ INDENT block+ DEDENT        # TypedBlock
   | NAME TYPESEP attribute* value=flow NEWLINE                                     # Field
   | paragraph                                                                      # PlainParagraph
   | NAME RECSEP description=flow NEWLINE+ INDENT (recordRow | NEWLINE)+ DEDENT     # RecordSet
   | INDENT ((BULLET paragraph+) | NEWLINE)+ DEDENT                                 # UnorderedList
   | INDENT ((HASH paragraph+) | NEWLINE)+ DEDENT                                   # OrderedList
   | '!!!(' text ')' NEWLINE block                                                  # Remark
   | '"""[' text ']' NEWLINE ( INDENT block+ DEDENT )                               # CitationBlock
   | '>>>(' text ')' attribute*                                                     # InsertFragment
   | '<<<(' text ')' attribute*                                                     # IncludeFile
   | NEWLINE                                                                        # Empty
   ;

declaration: '!' NAME TYPESEP description=flow NEWLINE ;

UNICODE_BOM: (UTF8_BOM
    | UTF16_BOM
    | UTF32_BOM
    ) -> skip
    ;

UTF8_BOM: '\uEFBBBF';
UTF16_BOM: '\uFEFF';
UTF32_BOM: '\u0000FEFF';

document: declaration* block* EOF ;

