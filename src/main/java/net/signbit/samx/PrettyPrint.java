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
import java.nio.file.Files;
import java.nio.file.Paths;

import net.signbit.samx.parser.SamXParser;

public final class PrettyPrint
{
   private static void checkMatch(String originalFileName, String pretty) throws IOException
   {
      final String original = new String(Files.readAllBytes(Paths.get(originalFileName)));

      if (original.equals(pretty))
      {
         System.err.println("OK: Prettified output matched input");
      }
      else
      {
         if (original.length() != pretty.length())
         {
            System.err.println(String.format("Input is %d characters, pretty is %d characters", original.length(), pretty.length()));
         }

         int compareLength = original.length();
         if (compareLength > pretty.length())
         {
            compareLength = pretty.length();
         }

         int lineNumber = 1;
         int columnNumber = 1;

         int ii = 0;

         while ((ii < compareLength) && (original.charAt(ii) == pretty.charAt(ii)))
         {
            if (original.charAt(ii) == '\n')
            {
               ++ lineNumber;
               columnNumber = 0;
            }
            ++ columnNumber;

            ++ ii;
         }

         System.err.println(String.format("Mismatch at offset %d (line %d, column %d): input has '%c', pretty has '%c'", ii, lineNumber, columnNumber, original.charAt(ii), pretty.charAt(ii)));
      }
   }

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

      final String pretty = builder.toString();

      System.out.println(pretty);

      checkMatch(args[0], pretty);
   }
}
