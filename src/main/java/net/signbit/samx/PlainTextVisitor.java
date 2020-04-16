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

import java.util.List;

import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;

import net.signbit.samx.parser.SamXLexer;
import net.signbit.samx.parser.SamXParser;
import net.signbit.samx.parser.SamXParserBaseVisitor;

public class PlainTextVisitor extends SamXParserBaseVisitor<StringBuilder>
{
   private BufferedTokenStream tokenStream;

   public PlainTextVisitor(BufferedTokenStream tokenStream)
   {
      this.tokenStream = tokenStream;
   }

   @Override
   public StringBuilder visitText(SamXParser.TextContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      boolean firstToken = true;

      for (ParseTree tn : ctx.children)
      {
         if (!firstToken)
         {
            addSpaceIfPresentInInput(builder, tn);
         }

         builder.append(tn.getText());
         firstToken = false;
      }

      return builder;
   }

   private void addSpaceIfPresentInInput(StringBuilder builder, ParseTree tn)
   {
      final Interval pos = tn.getSourceInterval();

      if (pos.a <= pos.b)
      {
         final List<Token> precedingTokens = tokenStream.getHiddenTokensToLeft(pos.a, SamXLexer.WHITESPACE);
         if ((precedingTokens != null) && (!precedingTokens.isEmpty()))
         {
            builder.append(' ');
         }
      }
   }
}
