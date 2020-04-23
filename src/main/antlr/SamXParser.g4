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

parser grammar SamXParser;

options { tokenVocab = SamXLexer;  }

@header {
package net.signbit.samx.parser;
}

@parser::members
{

   private java.util.HashMap<String, net.signbit.samx.Parser.Result> includedDocuments = null;
   private java.util.HashMap<String, java.io.IOException> includedExceptions = null;

   private java.util.HashMap<String, String> referencePaths = new java.util.HashMap<>();
   private java.io.File basePath = null;

   private int currentHeaderLength = 0;

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

nameList : NAME (COMMA NAME) + ;

conditionExpr :
   variable=NAME                                               # BooleanTrueCondition
   | variable=NAME EQUAL KW_TRUE                                # BooleanTrueCondition
   | variable=NAME EQUAL KW_FALSE                              # BooleanFalseCondition
   | BANG variable=NAME                                         # BooleanFalseCondition
   | variable=NAME oper=(EQUAL|NOT_EQ) value=NAME              # ComparisonCondition
   | variable=NAME KW_IN OPEN_PHR nameList CLOSE_PHR            # BelongsToSetCondition
   | variable=NAME KW_NOT KW_IN OPEN_PHR nameList CLOSE_PHR      # NotBelongsToSetCondition
   | OPEN_PAR firstCond=conditionExpr CLOSE_PAR KW_OR OPEN_PAR secondCond=conditionExpr CLOSE_PAR  # AlternativeCondition
   | OPEN_PAR firstCond=conditionExpr CLOSE_PAR KW_AND OPEN_PAR secondCond=conditionExpr CLOSE_PAR  # CombinedCondition
   ;

condition : STT_COND conditionExpr CLOSE_PAR ;

keyValuePair: key=NAME EQ_SGN value=NAME ;

path : (SLASH NAME) * ;

url : SCHEME SLASHSH (authority=NAME ATSGN)? host=NAME (TYPESEP port=INTEGER)? path (QUESTION keyValuePair (AMPERS keyValuePair)* )? (HASH frag=NAME)? ;

escapeSeq : ESCAPE ;

attribute :
   STT_NAME NAME CLOSE_PAR                  # NameAttr
   | STT_ID NAME CLOSE_PAR                  # IdentifierAttr
   | STT_CLASS NAME CLOSE_PAR               # ClassAttr
   | STT_LANG NAME CLOSE_PAR                # LanguageAttr
   | OPEN_SQR text CLOSE_SQR                # CitationAttr
   | STT_REFR NAME CLOSE_SQR                # ReferenceAttr
   ;

declaration: BANG NAME TYPESEP description=flow NEWLINE ;

lessThan : LT ;

greaterThan : GT ;

ampersand : AMPERS ;

quote : QUOT ;

string : STRING ;

literal : NAME | TOKEN | INTEGER | SLASH
   | KW_IN | KW_NOT | KW_OR | KW_AND | KW_TRUE | KW_FALSE
   | PLUS | COMMA | SEMI | PERIOD
   | OPEN_PAR | CLOSE_PAR | BANG | QUESTION | EQ_SGN | DOLLAR
   | BULL_T | HASH_T ;

entity : escapeSeq | lessThan | greaterThan | ampersand | quote ;

text : ( literal | entity | string ) + ;

annotation : STT_ANN flow CLOSE_PAR ;

phrase : OPEN_PHR text CLOSE_PHR annotation* attribute* condition? ;

localInsert : STT_LOCIN text CLOSE_PAR ;

inlineCode : APOSTR text APOSTR ;

flow : ( text | phrase | localInsert | url | inlineCode )+ ;

paragraph : ( flow NEWLINE )+ NEWLINE ;

headerRow
   locals [ int columnCount = 0; ]
   : ( COLSEP NAME { $ctx.columnCount ++; } )+ NEWLINE { currentHeaderLength = $ctx.columnCount; };

optionalFlow : flow? ;

recordData
   locals [ int columnCount = 0; ]
   : condition? ( COLSEP optionalFlow { $ctx.columnCount ++; } )+ NEWLINE
   {
      if (currentHeaderLength != $ctx.columnCount)
      {
         throw new ParseCancellationException("line " + $start.getLine() +
            ":" + $start.getCharPositionInLine() +
            " incorrect number of columns; expected " + currentHeaderLength +
            " but observed " + $ctx.columnCount);
      }
   };

recordSep
   locals [ int columnCount = 0; ]
   : (STT_TBL_SEP { $ctx.columnCount ++; })+ NEWLINE
   {
      if (currentHeaderLength < $ctx.columnCount)
      {
         throw new ParseCancellationException("line " + $start.getLine() +
            ":" + $start.getCharPositionInLine() +
            " incorrect number of columns; expected at most " + currentHeaderLength +
            " but observed " + $ctx.columnCount);
      }
   };

recordRow : recordData | recordSep ;

gridElement : COLSEP attribute* optionalFlow ;

spanGridElement : MUL_COLSEP attribute* optionalFlow ;

generalGridElement : gridElement | spanGridElement ;

gridHeaderRow
   locals [ int columnCount = 0; ]
   : attribute* ( gridElement { $ctx.columnCount ++; } )+ NEWLINE { currentHeaderLength = $ctx.columnCount; };

gridRecordRow
   locals [ int columnCount = 0; ]
   : attribute* condition? ( gridElement { $ctx.columnCount ++; } )+ NEWLINE
   {
      if (currentHeaderLength != $ctx.columnCount)
      {
         throw new ParseCancellationException("line " + $start.getLine() +
            ":" + $start.getCharPositionInLine() +
            " incorrect number of columns; expected " + currentHeaderLength +
            " but observed " + $ctx.columnCount);
      }
   };

generalGridHeaderSep : STT_HDR_SEP NEWLINE ;

generalGridRowData : attribute* condition? generalGridElement+ COLSEP ;

generalGridRow : (generalGridRowData | GEN_ROW_SEP) NEWLINE ;

generalGridHeader : generalGridRow+ generalGridHeaderSep ;

preciseRecordSep : (STT_TBL_SEP)+ PLUS NEWLINE ;

preciseGridRowData : attribute* condition? gridElement+ NEWLINE ;

preciseGridRow : preciseGridRowData | preciseRecordSep ;

externalCode : EXTCODE ;

listElement : condition? flow NEWLINE (separator=NEWLINE? INDENT block+ DEDENT)? ;

unorderedList : (BULLET listElement) NEWLINE* ((BULLET listElement) | NEWLINE)* ;

orderedList : (HASH listElement) NEWLINE* ((HASH listElement) | NEWLINE)* ;

block :
     NAME TYPESEP attribute* condition? description=flow? NEWLINE+ INDENT block+ DEDENT         # TypedBlock
   | NAME TYPESEP attribute* condition? value=flow NEWLINE                                      # Field
   | condition NEWLINE+ INDENT block+ DEDENT                                                    # ConditionalBlock
   | paragraph                                                                                  # PlainParagraph
   | NAME RECSEP attribute* condition? description=flow NEWLINE+ INDENT headerRow (recordRow | NEWLINE)+ DEDENT     # RecordSet
   | unorderedList                                                                              # UnorderedListBlock
   | orderedList                                                                                # OrderedListBlock
   | STT_RMK text CLOSE_PAR NEWLINE block                                                       # Remark
   | STT_CIT text CLOSE_SQR NEWLINE ( INDENT block+ DEDENT )                                    # CitationBlock
   | STT_INFRG name=NAME CLOSE_PAR attribute* condition?                                        # InsertFragment
   | STT_DEFRG name=NAME CLOSE_PAR attribute* condition? NEWLINE+ INDENT block+ DEDENT          # DefineFragment
   | STT_INCL reference=text CLOSE_PAR attribute* condition?    { parseFile($reference.text); } # IncludeFile
   | STT_IMAGE text CLOSE_PAR attribute* condition? description=flow?                           # InsertImage
   | CODE_MARKER language=text CLOSE_PAR attribute* condition? NEWLINE+ INDENT (externalCode? NEWLINE)+ DEDENT     # CodeBlock
   | STT_GRID attribute* condition? description=flow? NEWLINE+ INDENT gridHeaderRow (gridRecordRow | NEWLINE) + DEDENT # Grid
   | STT_GEN_GRID attribute* condition? description=flow? NEWLINE+ INDENT generalGridHeader? (generalGridRow | NEWLINE)+ DEDENT # GeneralGrid
   | STT_PREC_GRID attribute* condition? description=flow? NEWLINE+ INDENT (preciseGridRow | NEWLINE) + DEDENT # PreciseGrid
   | NEWLINE                                                                                    # Empty
   ;

document: declaration* block* EOF ;

