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

package net.signbit.samx;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.antlr.v4.runtime.*;

import net.signbit.samx.parser.SamXLexer;
import net.signbit.samx.parser.SamXParser;

public class Parser
{
   public static class Result
   {
      public File inputFile = null;
      public CommonTokenStream tokens = null;
      public SamXParser.DocumentContext document = null;

      public HashMap<String, Result> includedDocuments = new HashMap<>();
      public HashMap<String, IOException> includedExceptions = new HashMap<>();
      public HashMap<String, String> referencePaths;

      public int errorCount = 0;
   }

   public static Result parse(String inputFileName) throws IOException
   {
      File inputFile = new File(inputFileName);
      if (inputFile.exists())
      {
         return parse(inputFile);
      }
      else
      {
         throw new RuntimeException("Input file does not exist");
      }
   }

   public static Result parse(File inputFile) throws IOException
   {
      return parse(inputFile, new HashMap<>(), new HashMap<>());
   }

   public static class SAMErrorListener extends BaseErrorListener
   {
      int errorCount = 0;

      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e)
      {
         super.syntaxError(recognizer, offendingSymbol, line, charPositionInLine, msg, e);
         errorCount++;
      }
   }

   public static Result parse(File inputFile, HashMap<String, Result> includedDocuments, HashMap<String, IOException> includedExceptions) throws IOException
   {
      CharStream input = CharStreams.fromFileName(inputFile.getPath());

      Result result = new Result();
      result.inputFile = inputFile;
      result.includedDocuments = includedDocuments;
      result.includedExceptions = includedExceptions;

      SamXLexer lexer = new SamXLexer(input);

      result.tokens = new CommonTokenStream(lexer);

      SamXParser parser = new SamXParser(result.tokens);

      SAMErrorListener sel = new SAMErrorListener();
      parser.addErrorListener(sel);
      parser.setBasePath(inputFile.getParentFile());
      parser.setIncludeDictionary(includedDocuments);
      parser.setIncludeExceptionsDictionary(includedExceptions);

      result.document = parser.document();
      result.referencePaths = parser.getReferencePaths();
      result.errorCount = sel.errorCount;

      return result;
   }

   public static Result parseString(String inputString)
   {
      CharStream input = CharStreams.fromString(inputString);

      Result result = new Result();
      result.inputFile = null;
      result.includedDocuments = new HashMap<>();
      result.includedExceptions = new HashMap<>();

      SamXLexer lexer = new SamXLexer(input);

      result.tokens = new CommonTokenStream(lexer);

      SamXParser parser = new SamXParser(result.tokens);
      SAMErrorListener sel = new SAMErrorListener();

      parser.addErrorListener(sel);
      parser.setBasePath(null);
      parser.setIncludeDictionary(result.includedDocuments);
      parser.setIncludeExceptionsDictionary(result.includedExceptions);

      result.document = parser.document();
      result.referencePaths = parser.getReferencePaths();
      result.errorCount = sel.errorCount;

      return result;
   }
}
