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
import java.util.List;

import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;

import net.signbit.samx.parser.SamXLexer;

public class VisitorUtils
{
   private static class TestSequence
   {
      final String string;
      final int length;

      int index = 0;

      int line = 1;
      int column = 1;

      private TestSequence(String string)
      {
         this.string = string;
         this.length = string.length();
      }

      boolean isExhausted()
      {
         return index >= length;
      }

      char get()
      {
         return string.charAt(index);
      }

      void advance()
      {
         if (get() == '\n')
         {
            line++;
            column = 1;
         }
         else
         {
            column++;
         }
         index++;
      }

      boolean advancePastWhitespace()
      {
         boolean advanced = false;

         while (! isExhausted())
         {
            final char ch = get();
            if ((ch == ' ') || (ch == '\t') || (ch == '\n'))
            {
               advanced = true;
               advance();
            }
            else
            {
               break;
            }
         }

         return advanced;
      }

      void advancePastHeaderSeparator()
      {
         while (! isExhausted())
         {
            final char ch = get();
            if ((ch == '+') || (ch == '='))
            {
               advance();
            }
            else
            {
               break;
            }
         }
      }
   }


   public static int getTokenIndent(ParserRuleContext ctx, BufferedTokenStream tokenStream)
   {
      final Interval blockPosition = ctx.getSourceInterval();
      final List<Token> whitespacePrecedingBlockPosition = tokenStream.getHiddenTokensToLeft(blockPosition.a, SamXLexer.INDENTS);
      if ((whitespacePrecedingBlockPosition != null) && (! whitespacePrecedingBlockPosition.isEmpty()))
      {
         return whitespacePrecedingBlockPosition.get(0).getText().length();
      }
      else
      {
         return 0;
      }
   }

   public static boolean compareFilesExceptWhitespace(String originalString, String prettyString)
   {
      TestSequence original = new TestSequence(originalString);
      TestSequence pretty = new TestSequence(prettyString);

      int commonSubstring = 0;

      while ((! original.isExhausted()) && (! pretty.isExhausted()))
      {
         if (original.advancePastWhitespace())
         {
            continue;
         }
         if (pretty.advancePastWhitespace())
         {
            continue;
         }

         final char originalChr = original.get();
         final char prettyChr = pretty.get();

         if (originalChr == prettyChr)
         {
            commonSubstring++;

            if (originalChr == '+')
            {
               original.advancePastHeaderSeparator();
               pretty.advancePastHeaderSeparator();
            }
            else
            {
               original.advance();
               pretty.advance();
            }

            continue;
         }

         throw new RuntimeException(String.format("[stripped] Mismatch at offset %d (line %d, column %d): input has '%c', pretty has '%c' at offset %d (line %d, column %d)",
               original.index, original.line, original.column, originalChr, prettyChr, pretty.index, pretty.line, pretty.column));
      }

      // drain trailing space
      original.advancePastWhitespace();
      pretty.advancePastWhitespace();

      if (original.isExhausted() && pretty.isExhausted())
      {
         return true;
      }
      else
      {
         throw new RuntimeException(String.format("[stripped] File size mismatch; common substring is %d characters", commonSubstring));
      }
   }

   public static void checkMatch(String originalFileName, String pretty) throws IOException
   {
      final String original = new String(Files.readAllBytes(Paths.get(originalFileName)));

      if (original.equals(pretty))
      {
         System.err.println("OK: Prettified output matched input");
      }
      else
      {
         compareFilesExceptWhitespace(original, pretty);

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

         if (ii == compareLength)
         {
            System.err.println(String.format("The smaller file is a prefix of the larger file, but the lengths differ: original %d vs pretty %d", original.length(), pretty.length()));
         }
         else
         {
            System.err.println(String.format("Mismatch at offset %d (line %d, column %d): input has '%c', pretty has '%c'", ii, lineNumber, columnNumber, original.charAt(ii), pretty.charAt(ii)));
         }
      }
   }

   public static boolean isInteger(String s)
   {
      return isInteger(s, 10);
   }

   public static boolean isInteger(String s, int radix)
   {
      if (s.isEmpty())
      {
         return false;
      }
      for (int i = 0; i < s.length(); i++)
      {
         if (i == 0 && s.charAt(i) == '-')
         {
            if (s.length() == 1)
            {
               return false;
            }
            else
            {
               continue;
            }
         }
         if (Character.digit(s.charAt(i), radix) < 0)
         {
            return false;
         }
      }
      return true;
   }
}
