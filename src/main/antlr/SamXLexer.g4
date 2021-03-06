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

lexer grammar SamXLexer;

@header {
package net.signbit.samx.parser;
}

channels { WHITESPACE, COMMENTS, INDENTS }

tokens { INDENT, DEDENT, END, INVALID, BOL }

@lexer::members 
{
   private java.util.ArrayDeque<Token> tokens = new java.util.ArrayDeque<Token>();
   private java.util.Stack<Integer> indents = new java.util.Stack<>();

   private boolean prepareProcessingCode = false;
   private boolean prepareFreeIndent = false;

   private int codeIndentLevel = 0;
   private boolean allowFreeIndent = false;

   private boolean ignoreNewLinesInConditions = false;
   private int nestedParenthesesLevel = 0;

   private boolean ignoreNewLinesInPhrases = false;
   private int lastTokenPositionColumn = 0;

   private Token lastToken;

   @Override
   public void emit(Token t)
   {
      tokens.add(t);
      lastTokenPositionColumn = t.getCharPositionInLine() + t.getText().length() + 1;
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

   private void addSpace()
   {
      final int start = this.getCharIndex();
      CommonToken token = new CommonToken(this._tokenFactorySourcePair, SamXParser.SPACES, WHITESPACE, start, start + 1);
      token.setText(" ");
      token.setLine(_tokenStartLine);
      token.setCharPositionInLine(lastTokenPositionColumn);
      tokens.add(token);
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
      indent.setCharPositionInLine(0);

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

   private void addCodeIndent(int indentLevel)
   {
      java.lang.StringBuilder builder = new java.lang.StringBuilder(indentLevel + 1);
      for (int ii = 0; ii <= indentLevel; ++ii)
      {
         builder.append(' ');
      }

      final int start = this.getCharIndex();
      CommonToken token = new CommonToken(this._tokenFactorySourcePair, SamXParser.BOL, INDENTS, start, start + indentLevel);
      token.setText(builder.toString());
      token.setLine(_tokenStartLine + 1);
      token.setCharPositionInLine(0);
      tokens.add(token);
   }

   private int getIndent(String text)
   {
      final char[] tokenTextBytes = text.toCharArray();
      int thisIndent = 0;
      for (char ch: tokenTextBytes)
      {
         if (ch == ' ')
         {
            thisIndent ++;
         }
      }

      return thisIndent;
   }

}


ESCAPE : '\\' . ;

SPACES : [ \t]+ -> channel(WHITESPACE) ;

COMMENT : '%' ~[\r\n\f]* -> channel(COMMENTS) ;

NEWLINE
 : ( '\r'? '\n' | '\r' | '\f' ) SPACES?
   {
      if (ignoreNewLinesInConditions)
      {
         skip();
         return;
      }

      if (ignoreNewLinesInPhrases)
      {
         addSpace();
         skip();
         return;
      }

      final int thisIndent = getIndent(getText());

      final int next = _input.LA(1);
      if ((next == '\n') || (next == '\r'))
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
         addNewLine();

         if (allowFreeIndent)
         {
            addDedent();

            allowFreeIndent = false;
         }
      }
      else if (thisIndent > currentIndent)
      {
         addNewLine();

         if (prepareProcessingCode)
         {
            prepareProcessingCode = false;
            codeIndentLevel = currentIndent;
            mode(EXTERNAL_CODE);
            indents.push(thisIndent);
            addIndent();
         }
         else
         {
            if (prepareFreeIndent)
            {
               prepareFreeIndent = false;
               allowFreeIndent = true;

               /* Add indent token here to indicate the contained elements
                * but do not record this particular indent since it might be
                * deep inside the table due to the column alignment and
                * conditions.
                *
                * Instead, record a level just after the table level.
                */
               indents.push(currentIndent + 1);
               addIndent();
            }

            if (! allowFreeIndent)
            {
               indents.push(thisIndent);
               addIndent();
            }
         }
      }
      else
      {
         addNewLine();
         popIndents(thisIndent);

         allowFreeIndent = false;
      }

      skip();

      addCodeIndent(thisIndent);
   } ;

KW_NOT : 'not' ;

KW_IN : 'in' ;

KW_OR : 'or' ;

KW_AND : 'and' ;

KW_TRUE : 'true' ;

KW_FALSE : 'false' ;

STT_COND : '(?' { ignoreNewLinesInConditions = true; nestedParenthesesLevel = 1; } ;

STT_NAME : '(*' ;

STT_CLASS : '(.' ;

STT_ID : '(#' ;

STT_LANG : '(!' ;

STT_ANN : '(:' { ignoreNewLinesInConditions = true; nestedParenthesesLevel = 1; } ;

STT_REFR : '[*' ;

MUL_COLSEP : ('||' '|'* '-'* ) | ( '|-' '-'*  );

NAME : [-a-zA-Z_] [-a-zA-Z0-9_.]+ ;

INTEGER : [1-9] [0-9]+ ;

SCHEME : 'http' 's'? ':' ;

COMMA : ',' ;

SEMI : ';' ;

PERIOD : '.' ;

DOLLAR : '$' ;

TOKEN : [-a-zA-Z0-9_]+ ;

LT : '<' ;

GT : '>' ;

TYPESEP : ':' ;

RECSEP : '::' { prepareFreeIndent = true; };

COLSEP : '|' ;

BULLET : '*' ;

STT_PREC_GRID : '###' { prepareFreeIndent = true; };

HASH : '#' ;

OPEN_PHR : '{' { ignoreNewLinesInPhrases = true; };

CLOSE_PHR : '}' { ignoreNewLinesInPhrases = false; };

QUOT : '\'' ;

STRING : '"' ( '\\' . | ~[\\\r\n\f"] )* '"' ;

APOSTR : '`' ;

CODE_MARKER : '```(' { prepareProcessingCode = true; } ;

UNICODE_BOM: (UTF8_BOM
    | UTF16_BOM
    | UTF32_BOM
    ) -> skip
    ;

UTF8_BOM: '\uEFBBBF';
UTF16_BOM: '\uFEFF';
UTF32_BOM: '\u0000FEFF';

CLOSE_PAR : ')'
   {
      if (ignoreNewLinesInConditions)
      {
         nestedParenthesesLevel --;

         if (nestedParenthesesLevel == 0)
         {
            ignoreNewLinesInConditions = false;
         }
      }
   } ;

OPEN_PAR : '(' { if ( ignoreNewLinesInConditions) { nestedParenthesesLevel ++; } } ;

OPEN_SQR : '[' ;

CLOSE_SQR : ']' ;

EQ_SGN : '=' ;

EQUAL : '==' ;

NOT_EQ : '!=' ;

SLASH : '/' ;

SLASHSH : '//' ;

ATSGN : '@' ;

QUESTION : '?' ;

AMPERS : '&' ;

BANG : '!' ;

STT_GRID: '+++' { prepareFreeIndent = true; } ;

STT_GEN_GRID: '-+-' { prepareFreeIndent = true; } ;

PLUS : '+' ;

STT_LOCIN : '>($' ;

STT_RMK : '!!!(' ;

STT_CIT : '"""[' ;

STT_INFRG : '>>>(*' ;

STT_IMAGE : '>>>(image' ;

STT_DEFRG : '~~~(*' ;

STT_INCL : '<<<(' ;

STT_TBL_SEP : '+' '-'+ ;

STT_HDR_SEP : '+' ('='| '+' )+ '+' ;

GEN_ROW_SEP : '+' ('+' | '-' | ' ')+ ;

mode EXTERNAL_CODE ;

EXTCODE : (~'\n')+ ;

NEWLINE_EXTCODE
 : ( '\r'? '\n' | '\r' | '\f' ) SPACES?

   {
      final int thisIndent = getIndent(getText());

      if (thisIndent > codeIndentLevel)
      {
         addNewLine();
         addCodeIndent(thisIndent);
      }
      else
      {
         addNewLine();

         mode(DEFAULT_MODE);
      }

      skip();
   } ;
