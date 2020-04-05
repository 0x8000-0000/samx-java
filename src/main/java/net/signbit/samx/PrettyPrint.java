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

import java.io.IOException;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import net.signbit.samx.parser.SamXLexer;
import net.signbit.samx.parser.SamXParser;

public final class PrettyPrint
{
   public static void main(String[] args) throws IOException
   {
      CharStream input = CharStreams.fromFileName(args[0]);

      SamXLexer lexer = new SamXLexer(input);

      CommonTokenStream tokens = new CommonTokenStream(lexer);

      SamXParser parser = new SamXParser(tokens);

      SamXParser.DocumentContext document = parser.document();

      PrettyPrinterVisitor printer = new PrettyPrinterVisitor();

      StringBuilder builder = printer.visit(document);

      System.out.println(builder.toString());
   }
}
