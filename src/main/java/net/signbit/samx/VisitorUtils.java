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
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;

import net.signbit.samx.parser.SamXLexer;

public class VisitorUtils
{
   public static int getTokenIndent(ParserRuleContext ctx, BufferedTokenStream tokenStream)
   {
      final Interval blockPosition = ctx.getSourceInterval();
      final List<Token> whitespacePrecedingBlockPosition = tokenStream.getHiddenTokensToLeft(blockPosition.a, SamXLexer.INDENTS);
      if ((whitespacePrecedingBlockPosition != null) && (! whitespacePrecedingBlockPosition.isEmpty()))
      {
         return  whitespacePrecedingBlockPosition.get(0).getText().length();
      }
      else
      {
         return 0;
      }
   }
}