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

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import net.signbit.samx.parser.SamXLexer;
import net.signbit.samx.parser.SamXParser;

public class Parser
{
   public static class Result
   {
      public SamXParser.DocumentContext document;

      public HashMap<String, SamXParser.DocumentContext> includedDocuments;
      public HashMap<String, IOException> includedExceptions;
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
      CharStream input = CharStreams.fromFileName(inputFile.getPath());

      SamXLexer lexer = new SamXLexer(input);

      CommonTokenStream tokens = new CommonTokenStream(lexer);

      SamXParser parser = new SamXParser(tokens);

      parser.setReferencePath(inputFile.getParentFile());
      
      Result result = new Result();
      result.document = parser.document();
      result.includedDocuments = parser.includedDocuments;
      result.includedExceptions = parser.includedExceptions;

      return result;
   }
}
