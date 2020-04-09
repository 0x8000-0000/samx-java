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

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

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
      final SamXParser.ConditionContext cond = ctx.condition();
      if (cond != null)
      {
         builder.append(visit(cond));
      }
      for (SamXParser.AttributeContext ac : ctx.attribute())
      {
         builder.append(visit(ac));
      }
      if (ctx.description != null)
      {
         builder.append(' ');
         builder.append(visit(ctx.description));
      }
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

   @Override
   public StringBuilder visitUnorderedList(SamXParser.UnorderedListContext ctx)
   {
      return visitList("* ", ctx.listElement());
   }

   @Override
   public StringBuilder visitOrderedList(SamXParser.OrderedListContext ctx)
   {
      return visitList("# ", ctx.listElement());
   }

   private StringBuilder visitList(String type, List<SamXParser.ListElementContext> elements)
   {
      StringBuilder builder = new StringBuilder();

      final int savedIndent = indentLevel;

      final int thisIndent = savedIndent + 1;

      for (SamXParser.ListElementContext lec: elements)
      {
         StringBuilder childBuilder = visit(lec);
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
   public StringBuilder visitListElement(SamXParser.ListElementContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      final SamXParser.ConditionContext cond = ctx.condition();
      if (cond != null)
      {
         builder.append(visit(cond));
         builder.append(' ');
      }

      for (SamXParser.ParagraphContext pc: ctx.paragraph())
      {
         builder.append(visit(pc));
      }

      return builder;
   }

   @Override
   public StringBuilder visitRecordSet(SamXParser.RecordSetContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      addIndent(builder);
      builder.append(ctx.NAME().getText());
      builder.append(ctx.RECSEP().getText());
      final SamXParser.ConditionContext cond = ctx.condition();
      if (cond != null)
      {
         builder.append(visit(cond));
      }
      for (SamXParser.AttributeContext ac : ctx.attribute())
      {
         builder.append(visit(ac));
      }
      if (ctx.description != null)
      {
         builder.append(' ');
         builder.append(visit(ctx.description));
      }
      builder.append('\n');
      builder.append('\n');

      indentLevel++;

      builder.append(visit(ctx.headerRow()));
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
   public StringBuilder visitHeaderRow(SamXParser.HeaderRowContext ctx)
   {
      StringBuilder builder = new StringBuilder();
      addIndent(builder);

      boolean firstToken = true;
      for (TerminalNode tc: ctx.NAME())
      {
         if (firstToken)
         {
            firstToken = false;
         }
         else
         {
            builder.append(" ");
         }

         builder.append("| ");
         builder.append(tc.getText());
      }
      builder.append('\n');

      return builder;
   }

   @Override
   public StringBuilder visitRecordRow(SamXParser.RecordRowContext ctx)
   {
      StringBuilder builder = new StringBuilder();
      addIndent(builder);

      final SamXParser.ConditionContext cond = ctx.condition();
      if (cond != null)
      {
         builder.append(visit(cond));
         builder.append(" ");
      }

      boolean firstToken = true;

      for (SamXParser.FlowContext tc: ctx.flow())
      {
         if (firstToken)
         {
            firstToken = false;
         }
         else
         {
            builder.append(" ");
         }

         StringBuilder childBuilder = visit(tc);
         if (childBuilder != null)
         {
            builder.append("| ");
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
      for (SamXParser.AttributeContext ac : ctx.attribute())
      {
         builder.append(visit(ac));
      }
      final SamXParser.ConditionContext cond = ctx.condition();
      if (cond != null)
      {
         builder.append(visit(cond));
      }

      return builder;
   }

   @Override
   public StringBuilder visitAnnotation(SamXParser.AnnotationContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      builder.append("(:");
      builder.append(visit(ctx.text()));
      builder.append(')');

      return builder;
   }

   private StringBuilder visitAttribute(char sigil, String text)
   {
      StringBuilder builder = new StringBuilder();

      builder.append('(');
      builder.append(sigil);
      builder.append(text);
      builder.append(')');

      return builder;
   }

   @Override
   public StringBuilder visitIdAttr(SamXParser.IdAttrContext ctx)
   {
      return visitAttribute('*', ctx.NAME().getText());
   }

   @Override
   public StringBuilder visitLanguageAttr(SamXParser.LanguageAttrContext ctx)
   {
      return visitAttribute('!', ctx.NAME().getText());
   }

   @Override
   public StringBuilder visitCitation(SamXParser.CitationContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      builder.append('[');
      builder.append(visit(ctx.text()));
      builder.append(']');

      return builder;
   }

   @Override
   public StringBuilder visitField(SamXParser.FieldContext ctx)
   {
      StringBuilder builder = new StringBuilder();
      addIndent(builder);

      builder.append(ctx.NAME().getText());
      builder.append(ctx.TYPESEP().getText());
      for (SamXParser.AttributeContext ac : ctx.attribute())
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

      for (SamXParser.DeclarationContext dc: ctx.declaration())
      {
         StringBuilder childBuilder = visit(dc);
         if (childBuilder != null)
         {
            builder.append(childBuilder);
         }
      }

      for (SamXParser.BlockContext bc : ctx.block())
      {
         StringBuilder childBuilder = visit(bc);
         if (childBuilder != null)
         {
            builder.append(childBuilder);
         }
      }

      return builder;
   }

   @Override
   public StringBuilder visitDeclaration(SamXParser.DeclarationContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      builder.append('!');
      builder.append(ctx.NAME());
      builder.append(':');
      builder.append(' ');
      builder.append(visit(ctx.description));
      builder.append('\n');

      return builder;
   }

   @Override
   public StringBuilder visitUrl(SamXParser.UrlContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      builder.append(ctx.SCHEME().getText());
      builder.append("//");
      builder.append(ctx.host.getText());
      builder.append(ctx.path().getText());

      return builder;
   }

   @Override
   public StringBuilder visitCodeBlock(SamXParser.CodeBlockContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      addIndent(builder);
      builder.append("```(");
      builder.append(visit(ctx.language));
      builder.append(")");
      builder.append('\n');

      indentLevel ++;
      for (SamXParser.ExternalCodeContext ecc: ctx.externalCode())
      {
         addIndent(builder);
         int codeIndent = Integer.valueOf(ecc.CODE_INDENT().getText());
         for (int ii = 0; ii < codeIndent; ++ii)
         {
            builder.append(' ');
         }
         builder.append(ecc.EXTCODE().getText());
         builder.append('\n');
      }
      indentLevel --;

      return builder;
   }

   @Override
   public StringBuilder visitCondition(SamXParser.ConditionContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      builder.append("(?");
      builder.append(visit(ctx.conditionExpr()));
      builder.append(")");

      return builder;
   }

   @Override
   public StringBuilder visitBooleanTrueCondition(SamXParser.BooleanTrueConditionContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      builder.append(ctx.variable.getText());

      return builder;
   }

   @Override
   public StringBuilder visitBooleanFalseCondition(SamXParser.BooleanFalseConditionContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      builder.append('!');
      builder.append(ctx.variable.getText());

      return builder;
   }

   @Override
   public StringBuilder visitComparisonCondition(SamXParser.ComparisonConditionContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      builder.append(ctx.variable.getText());
      builder.append(ctx.oper.getText());
      builder.append(ctx.value.getText());

      return builder;
   }

   @Override
   public StringBuilder visitBelongsToSetCondition(SamXParser.BelongsToSetConditionContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      builder.append(ctx.variable.getText());
      builder.append(" in {");
      builder.append(visit(ctx.nameList()));
      builder.append("}");

      return builder;
   }

   @Override
   public StringBuilder visitNotBelongsToSetCondition(SamXParser.NotBelongsToSetConditionContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      builder.append(ctx.variable.getText());
      builder.append(" not in {");
      builder.append(visit(ctx.nameList()));
      builder.append("}");

      return builder;
   }

   @Override
   public StringBuilder visitNameList(SamXParser.NameListContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      boolean firstToken = true;
      for (TerminalNode tn: ctx.NAME())
      {
         if (firstToken)
         {
            firstToken = false;
         }
         else
         {
            builder.append(", ");
         }
         builder.append(tn.getText());
      }

      return builder;
   }

   private StringBuilder visitConditionWithOperator(String operator, SamXParser.ConditionExprContext firstCond, SamXParser.ConditionExprContext secondCond)
   {
      StringBuilder builder = new StringBuilder();

      builder.append("(");
      builder.append(visit(firstCond));
      builder.append(") ");
      builder.append(operator);
      builder.append(" (");
      builder.append(visit(secondCond));
      builder.append(")");

      return builder;
   }

   @Override
   public StringBuilder visitAlternativeCondition(SamXParser.AlternativeConditionContext ctx)
   {
      return visitConditionWithOperator("or", ctx.firstCond, ctx.secondCond);
   }

   @Override
   public StringBuilder visitCombinedCondition(SamXParser.CombinedConditionContext ctx)
   {
      return visitConditionWithOperator("and", ctx.firstCond, ctx.secondCond);
   }
}
