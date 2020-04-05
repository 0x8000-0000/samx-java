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

import org.antlr.v4.runtime.tree.ParseTree;

import net.signbit.samx.parser.SamXBaseVisitor;
import net.signbit.samx.parser.SamXParser;

public class PrettyPrinterVisitor extends SamXBaseVisitor<StringBuilder>
{
   private int indentLevel = 0;

   private void addIndent(StringBuilder builder)
   {
      for (int ii = 0; ii < indentLevel; ++ii)
      {
         builder.append("   ");
      }
   }

   @Override
   public StringBuilder visitParagraph(SamXParser.ParagraphContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      addIndent(builder);

      boolean firstToken = true;
      for (SamXParser.FlowContext text : ctx.flow())
      {
         if (firstToken)
         {
            firstToken = false;
         }
         else
         {
            builder.append(' ');
         }
         builder.append(visit(text));
      }

      builder.append('\n');
      builder.append('\n');

      return builder;
   }

   @Override
   public StringBuilder visitTypedBlock(SamXParser.TypedBlockContext ctx)
   {
      StringBuilder builder = new StringBuilder();
      addIndent(builder);

      builder.append(ctx.NAME().getText());
      builder.append(ctx.TYPESEP().getText());
      builder.append(visit(ctx.description));
      builder.append('\n');
      builder.append('\n');
      indentLevel++;
      for (SamXParser.BlockContext bc : ctx.block())
      {
         StringBuilder childBuilder = visit(bc);
         if (childBuilder != null)
         {
            builder.append(childBuilder);
         }
      }
      indentLevel--;

      return builder;
   }


   /*
   @Override
   public StringBuilder visitIndentedBlock(SamXParser.IndentedBlockContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      indentLevel++;
      for (ParseTree pt : ctx.children)
      {
         StringBuilder childBuilder = visit(pt);
         if (childBuilder != null)
         {
            builder.append(childBuilder);
         }
      }
      indentLevel--;

      return builder;
   }
    */

   @Override
   public StringBuilder visitUnorderedList(SamXParser.UnorderedListContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      final int savedIndent = indentLevel;

      final int thisIndent = savedIndent + 1;

      for (ParseTree pt : ctx.children)
      {
         StringBuilder childBuilder = visit(pt);
         if (childBuilder != null)
         {
            indentLevel = thisIndent;
            addIndent(builder);
            indentLevel = 0;
            builder.append("* ");
            builder.append(childBuilder);
         }
      }

      indentLevel = savedIndent;

      return builder;
   }

   @Override
   public StringBuilder visitRecordSet(SamXParser.RecordSetContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      addIndent(builder);
      builder.append(ctx.NAME().getText());
      builder.append(ctx.RECSEP().getText());
      builder.append(visit(ctx.description));
      builder.append('\n');
      builder.append('\n');
      indentLevel++;
      for (SamXParser.RecordRowContext pt : ctx.recordRow())
      {
         StringBuilder childBuilder = visit(pt);
         if (childBuilder != null)
         {
            builder.append(childBuilder);
         }
      }
      indentLevel--;

      return builder;
   }

   @Override
   public StringBuilder visitRecordRow(SamXParser.RecordRowContext ctx)
   {
      StringBuilder builder = new StringBuilder();
      addIndent(builder);
      for (SamXParser.FlowContext tc: ctx.flow())
      {
         StringBuilder childBuilder = visit(tc);
         if (childBuilder != null)
         {
            builder.append('|');
            builder.append(childBuilder);
         }
      }
      builder.append('\n');
      return builder;
   }

   @Override
   public StringBuilder visitText(SamXParser.TextContext ctx)
   {
      StringBuilder builder = new StringBuilder();
      for (ParseTree tn : ctx.children)
      {
         if (builder.length() > 0)
         {
            builder.append(' ');
         }
         builder.append(tn.getText());
      }
      return builder;
   }

   @Override
   public StringBuilder visitFlow(SamXParser.FlowContext ctx)
   {
      StringBuilder builder = new StringBuilder();
      for (ParseTree tn : ctx.children)
      {
         StringBuilder childBuilder = visit(tn);
         if (childBuilder != null)
         {
            if (builder.length() > 0)
            {
               builder.append(' ');
            }
            builder.append(childBuilder);
         }
      }
      return builder;
   }

   @Override
   public StringBuilder visitPhrase(SamXParser.PhraseContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      builder.append('{');
      builder.append(visit(ctx.text()));
      builder.append('}');
      for (SamXParser.AnnotationContext ac : ctx.annotation())
      {
         builder.append(visit(ac));
      }

      return builder;
   }

   @Override
   public StringBuilder visitAnnotation(SamXParser.AnnotationContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      builder.append('(');
      builder.append(visit(ctx.text()));
      builder.append(')');

      return builder;
   }

   private StringBuilder visitAttribute(char sigil, ParseTree pt)
   {
      StringBuilder builder = new StringBuilder();

      builder.append('(');
      builder.append(sigil);
      builder.append(visit(pt));
      builder.append(')');

      return builder;
   }

   @Override
   public StringBuilder visitConditionAttr(SamXParser.ConditionAttrContext ctx)
   {
      return visitAttribute('?', ctx.text());
   }

   @Override
   public StringBuilder visitIdAttr(SamXParser.IdAttrContext ctx)
   {
      return visitAttribute('*', ctx.NAME());
   }

   @Override
   public StringBuilder visitField(SamXParser.FieldContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      addIndent(builder);
      builder.append(ctx.NAME().getText());
      builder.append(ctx.TYPESEP().getText());
      for (SamXParser.AttributeContext ac: ctx.attribute())
      {
         builder.append(visit(ac));
      }
      builder.append(' ');
      builder.append(visit(ctx.flow()));
      builder.append('\n');

      return builder;
   }

   @Override
   public StringBuilder visitEmpty(SamXParser.EmptyContext ctx)
   {
      return null;
   }

   @Override
   public StringBuilder visitDocument(SamXParser.DocumentContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      for (SamXParser.BlockContext pt : ctx.block())
      {
         StringBuilder childBuilder = visit(pt);
         if (childBuilder != null)
         {
            builder.append(childBuilder);
         }
      }

      return builder;
   }
}
