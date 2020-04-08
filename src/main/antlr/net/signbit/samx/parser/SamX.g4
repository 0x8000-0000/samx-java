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

tokens { INDENT, DEDENT, END, INVALID, CODE_INDENT }

@lexer::members 
{

   private java.util.ArrayDeque<Token> tokens = new java.util.ArrayDeque<Token>();
   private java.util.Stack<Integer> indents = new java.util.Stack<>();

   private boolean prepareProcessingCode = false;
   private boolean processingCode = false;

   private int thisIndent = 0;
   private int codeIndentBase = 0;

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

   private void addCodeIndent()
   {
      CommonToken codeIndent = makeToken(SamXParser.CODE_INDENT, java.lang.Integer.toString(thisIndent - codeIndentBase));
      codeIndent.setLine(_tokenStartLine);
      codeIndent.setCharPositionInLine(_tokenStartCharPositionInLine);

      tokens.add(codeIndent);
   }

}

@parser::members
{

   private java.util.HashMap<String, net.signbit.samx.Parser.Result> includedDocuments = null;
   private java.util.HashMap<String, java.io.IOException> includedExceptions = null;

   private java.util.HashMap<String, String> referencePaths = new java.util.HashMap<>();
   private java.io.File basePath = null;

   public void setBasePath(java.io.File aPath)
   {
      basePath = aPath;
   }

   public java.util.HashMap<String, String> getReferencePaths()
   {
      return referencePaths;
   }

   public void setIncludeDictionary(java.util.HashMap<String, net.signbit.samx.Parser.Result> aDict)
   {
      includedDocuments = aDict;
   }

   public void setIncludeExceptionsDictionary(java.util.HashMap<String, java.io.IOException> aDict)
   {
      includedExceptions = aDict;
   }

   private void parseFile(String reference)
   {
      java.io.File includeFile = new java.io.File(basePath, reference);

      if (includeFile.exists())
      {
         referencePaths.put(reference, includeFile.getAbsolutePath());

         if (! includedDocuments.containsKey(includeFile.getAbsolutePath()))
         {
            try
            {
               net.signbit.samx.Parser.Result result = net.signbit.samx.Parser.parse(includeFile,
                  includedDocuments,
                  includedExceptions);

               includedDocuments.put(includeFile.getAbsolutePath(), result);
            }
            catch (java.io.IOException ioe)
            {
               includedExceptions.put(includeFile.getAbsolutePath(), ioe);
            }
         }
      }
      else
      {
         includedExceptions.put(includeFile.getAbsolutePath(), new java.io.FileNotFoundException(includeFile.getAbsolutePath()));
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
      thisIndent = 0;
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
         addNewLine();

         if (! processingCode)
         {
            indents.push(thisIndent);
            addIndent();

            if (prepareProcessingCode)
            {
               processingCode = true;
               prepareProcessingCode = false;
               codeIndentBase = thisIndent;
            }
         }

         skip();
      }
      else
      {
         addNewLine();

         popIndents(thisIndent);

         processingCode = false;
         skip();
      }
   } ;

nameList : NAME (',' NAME) + ;

condition_expr :
   variable=NAME                                               # BooleanTrueCondition
   | variable=NAME EQUAL 'true'                                # BooleanTrueCondition
   | variable=NAME EQUAL 'false'                               # BooleanFalseCondition
   | '!' variable=NAME                                         # BooleanFalseCondition
   | variable=NAME oper=(EQUAL|NOT_EQ) value=NAME              # ComparisonCondition
   | variable=NAME 'in' OPEN_PHR nameList CLOSE_PHR            # BelongsToSetCondition
   | variable=NAME 'not' 'in' OPEN_PHR nameList CLOSE_PHR      # NotBelongsToSetCondition
   | OPEN_PAR firstCond=condition_expr CLOSE_PAR 'or' OPEN_PAR secondCond=condition_expr CLOSE_PAR  # AlternativeCondition
   | OPEN_PAR firstCond=condition_expr CLOSE_PAR 'and' OPEN_PAR secondCond=condition_expr CLOSE_PAR  # CombinedCondition
   ;

condition : STT_COND condition_expr CLOSE_PAR ;

NAME : [-a-zA-Z_] [-a-zA-Z0-9_.]+ ;

INTEGER : [1-9] [0-9]+ ;

SCHEME : 'http' 's'? ':' ;

keyValuePair: key=NAME EQUAL value=NAME ;

path : ('/' NAME) * ;

url : SCHEME '//' (authority=NAME '@')? host=NAME (TYPESEP port=INTEGER)? path ('?' keyValuePair ('&' keyValuePair)* )? ('#' frag=NAME)? ;

TOKEN : [-a-zA-Z0-9_]+ | '.' | ',' | '&' | ';' ;

TYPESEP : ':' ;

RECSEP : '::' ;

COLSEP : '|' ;

BULLET : '*' ;

HASH : '#' ;

OPEN_PHR : '{' ;

CLOSE_PHR : '}' ;

ESCAPE : '\\' ;

QUOT : '\'' ;

STRING : '"' ( '\\' . | ~[\\\r\n\f"] )* '"' ;

escapeSeq : ESCAPE . ;

attribute :
   STT_NAME CLOSE_PAR                           # NameAttr
   | STT_ID NAME CLOSE_PAR                      # IdAttr
   | STT_LANG '!' NAME CLOSE_PAR                # LanguageAttr
   | '[' text ']'                               # Citation
   | '[*' blockName=NAME '/' idName=NAME ']'    # Reference
   ;

text : ( NAME | TOKEN | INTEGER | STRING | '/' | escapeSeq | 'in' | 'not' | 'or' | 'and' | 'true' | 'false' | QUOT | '+' | ',' | OPEN_PAR | CLOSE_PAR ) + ;

STT_COND : '(?' ;

STT_NAME : '(#' ;

STT_ID : '(*' ;

STT_LANG : '(!' ;

STT_ANN : '(:' ;

annotation : STT_ANN text CLOSE_PAR ;

phrase : OPEN_PHR text CLOSE_PHR annotation* attribute* condition? ;

localInsert : '>($' text CLOSE_PAR ;

APOSTR : '`' ;

inlineCode : APOSTR text APOSTR ;

flow : ( text | phrase | localInsert | url | inlineCode )+ ;

paragraph : ( flow NEWLINE )+ NEWLINE ;

headerRow : ( COLSEP NAME )+ NEWLINE ;

recordRow : condition? ( COLSEP flow )+ NEWLINE ;

EXTCODE : (~'\n')+ { processingCode }? { addCodeIndent(); };

CODE_MARKER : '```(' { prepareProcessingCode = true; };

externalCode : CODE_INDENT EXTCODE ;

listElement : condition? paragraph+ ;

block :
     NAME TYPESEP attribute* condition? description=flow? NEWLINE+ INDENT block+ DEDENT        # TypedBlock
   | NAME TYPESEP attribute* condition? value=flow NEWLINE                                     # Field
   | paragraph                                                                         # PlainParagraph
   | NAME RECSEP condition? description=flow NEWLINE+ INDENT headerRow (recordRow | NEWLINE)+ DEDENT     # RecordSet
   | INDENT ((BULLET listElement) | NEWLINE)+ DEDENT                         # UnorderedList
   | INDENT ((HASH listElement) | NEWLINE)+ DEDENT                           # OrderedList
   | '!!!(' text ')' NEWLINE block                                                     # Remark
   | '"""[' text ']' NEWLINE ( INDENT block+ DEDENT )                                  # CitationBlock
   | '>>>(' NAME ')' attribute* condition?                                             # InsertFragment
   | '~~~(' NAME ')' attribute* condition? NEWLINE+ INDENT block+ DEDENT               # DefineFragment
   | '<<<(' reference=text ')' attribute* condition?    { parseFile($reference.text); }     # IncludeFile
   | CODE_MARKER language=text ')' attribute* condition? NEWLINE+ INDENT (externalCode? NEWLINE)+ DEDENT     # CodeBlock
   | NEWLINE                                                                           # Empty
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

CLOSE_PAR : ')' ;

OPEN_PAR : '(' ;

EQUAL : '=' ;

NOT_EQ : '!=' ;
