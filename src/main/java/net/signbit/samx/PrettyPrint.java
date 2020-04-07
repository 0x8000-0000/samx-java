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

import net.signbit.samx.parser.SamXParser;

public final class PrettyPrint
{
   public static void main(String[] args) throws IOException
   {
      if (args.length < 1)
      {
         System.err.println("Required argument missing");
         return;
      }

      SamXParser.DocumentContext document = Parser.parse(args[0]).document;

      PrettyPrinterVisitor printer = new PrettyPrinterVisitor();

      StringBuilder builder = printer.visit(document);

      System.out.println(builder.toString());
   }
}
