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

package net.signbit.samx.parser;

import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SamXLexerTest
{
   private CommonTokenStream tokenizeString(String inputString)
   {
      CharStream inputStream = CharStreams.fromString(inputString);

      SamXLexer lexer = new SamXLexer(inputStream);

      CommonTokenStream tokenStream = new CommonTokenStream(lexer);

      tokenStream.fill();

      return tokenStream;
   }

   private int countTokens(List<Token> tokenStream, int type)
   {
      int count = 0;
      for (Token tt : tokenStream)
      {
         if (tt.getType() == type)
         {
            count++;
         }
      }
      return count;
   }

   private boolean balancedIndentsDedents(List<Token> tokenStream)
   {
      int indentLevel = 0;

      for (Token tt : tokenStream)
      {
         if (tt.getType() == SamXParser.INDENT)
         {
            indentLevel++;
         }
         else if (tt.getType() == SamXParser.DEDENT)
         {
            indentLevel--;
         }
      }

      return indentLevel == 0;
   }

   private boolean tokensInNaturalOrder(List<Token> tokenStream)
   {
      int lastLine = 0;
      int lastColumn = 0;

      for (Token tt : tokenStream)
      {
         final int thisLine = tt.getLine();
         if (thisLine < lastLine)
         {
            return false;
         }
         else
         {
            final int thisColumn = tt.getCharPositionInLine();

            if (thisLine == lastLine)
            {
               if (thisColumn < lastColumn)
               {
                  return false;
               }
               lastColumn = thisColumn;
            }
            else
            {
               lastLine = thisLine;
               lastColumn = thisColumn;
            }
         }
      }

      return true;
   }

   @Test
   public void parseSimpleSentence()
   {
      final String simpleSentence = "This is a sentence.\n";
      CommonTokenStream tokenStream = tokenizeString(simpleSentence);
      final List<Token> tokens = tokenStream.getTokens();

      assertEquals(7, tokens.size());

      assertEquals(2, countTokens(tokens, SamXParser.NEWLINE));

      assertTrue("Natural order", tokensInNaturalOrder(tokens));
      assertTrue("Balanced indents", balancedIndentsDedents(tokens));
   }

   @Test
   public void parseOneLevelIndent()
   {
      final String simpleSentence =
            "" +
                  "Level1\n" +
                  "   Level2\n";
      CommonTokenStream tokenStream = tokenizeString(simpleSentence);
      final List<Token> tokens = tokenStream.getTokens();

      assertEquals(8, tokens.size());

      assertEquals(1, countTokens(tokenStream.getTokens(), SamXParser.INDENT));
      assertEquals(1, countTokens(tokenStream.getTokens(), SamXParser.DEDENT));

      assertTrue("Natural order", tokensInNaturalOrder(tokens));
      assertTrue("Balanced indents", balancedIndentsDedents(tokenStream.getTokens()));
   }

   @Test
   public void parseTwoLevelIndents()
   {
      final String simpleSentence =
            "" +
                  "Level1\n" +
                  "   Level2\n" +
                  "      Level3\n";
      CommonTokenStream tokenStream = tokenizeString(simpleSentence);
      final List<Token> tokens = tokenStream.getTokens();

      assertEquals(12, tokens.size());

      assertEquals(2, countTokens(tokens, SamXParser.INDENT));
      assertEquals(2, countTokens(tokens, SamXParser.DEDENT));

      assertTrue("Natural order", tokensInNaturalOrder(tokens));
      assertTrue("Balanced indents", balancedIndentsDedents(tokens));
   }

   @Test
   public void parseTwoLevelIndentsWithUnindent()
   {
      final String simpleSentence =
            "" +
                  "Level1\n" +
                  "   Level2\n" +
                  "      Level3\n" +
                  "   Again2\n";

      CommonTokenStream tokenStream = tokenizeString(simpleSentence);
      final List<Token> tokens = tokenStream.getTokens();

      assertEquals(2, countTokens(tokens, SamXParser.INDENT));
      assertEquals(2, countTokens(tokens, SamXParser.DEDENT));

      assertTrue("Natural order", tokensInNaturalOrder(tokens));
      assertTrue("Balanced indents", balancedIndentsDedents(tokens));
   }

   @Test
   public void parseTwoLevelIndentsWithMultipleLines()
   {
      final String simpleSentence =
            "" +
                  "Level1\n" +
                  "   Level2\n" +
                  "\n" +
                  "   Still2\n" +
                  "      Level3\n" +
                  "   Again2\n";

      CommonTokenStream tokenStream = tokenizeString(simpleSentence);
      final List<Token> tokens = tokenStream.getTokens();

      assertEquals(2, countTokens(tokens, SamXParser.INDENT));
      assertEquals(2, countTokens(tokens, SamXParser.DEDENT));

      assertTrue("Natural order", tokensInNaturalOrder(tokens));
      assertTrue("Balanced indents", balancedIndentsDedents(tokens));
   }

   @Test
   public void ignoreSpaceOnlyLinesLess()
   {
      final String simpleSentence =
            "" +
                  "Level1\n" +
                  "   Level2\n" +
                  " \n" +
                  "   Still2\n" +
                  "      Level3\n" +
                  "   Again2\n";

      CommonTokenStream tokenStream = tokenizeString(simpleSentence);
      final List<Token> tokens = tokenStream.getTokens();

      assertEquals(2, countTokens(tokens, SamXParser.INDENT));
      assertEquals(2, countTokens(tokens, SamXParser.DEDENT));

      assertTrue("Natural order", tokensInNaturalOrder(tokens));
      assertTrue("Balanced indents", balancedIndentsDedents(tokens));
   }

   @Test
   public void ignoreSpaceOnlyLinesEqual()
   {
      final String simpleSentence =
            "" +
                  "Level1\n" +
                  "   Level2\n" +
                  "   \n" +
                  "   Still2\n" +
                  "      Level3\n" +
                  "   Again2\n";

      CommonTokenStream tokenStream = tokenizeString(simpleSentence);
      final List<Token> tokens = tokenStream.getTokens();

      assertEquals(2, countTokens(tokens, SamXParser.INDENT));
      assertEquals(2, countTokens(tokens, SamXParser.DEDENT));

      assertTrue("Natural order", tokensInNaturalOrder(tokens));
      assertTrue("Balanced indents", balancedIndentsDedents(tokens));
   }

   @Test
   public void ignoreSpaceOnlyLinesMore()
   {
      final String simpleSentence =
            "" +
                  "Level1\n" +
                  "   Level2\n" +
                  "      \n" +
                  "   Still2\n" +
                  "      Level3\n" +
                  "   Again2\n";

      CommonTokenStream tokenStream = tokenizeString(simpleSentence);
      final List<Token> tokens = tokenStream.getTokens();

      assertEquals(2, countTokens(tokens, SamXParser.INDENT));
      assertEquals(2, countTokens(tokens, SamXParser.DEDENT));

      assertTrue("Natural order", tokensInNaturalOrder(tokens));
      assertTrue("Balanced indents", balancedIndentsDedents(tokens));
   }
}
