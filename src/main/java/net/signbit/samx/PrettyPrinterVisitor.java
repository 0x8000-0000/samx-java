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

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.text.StringTokenizer;
import org.apache.commons.text.WordUtils;

import net.signbit.samx.parser.SamXLexer;
import net.signbit.samx.parser.SamXParser;
import net.signbit.samx.parser.SamXParserBaseVisitor;


public class PrettyPrinterVisitor extends SamXParserBaseVisitor<StringBuilder>
{
   private int indentLevel = 0;
   private BufferedTokenStream tokenStream;

   private static final String indentString = "   ";
   private int wrapParagraphAtColumn = 72;

   private void addIndent(StringBuilder builder)
   {
      for (int ii = 0; ii < indentLevel; ++ii)
      {
         builder.append(indentString);
      }
   }

   private StringBuilder visitParagraphDirect(SamXParser.ParagraphContext ctx)
   {
      StringBuilder builder = new StringBuilder();

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

      return builder;
   }

   @Override
   public StringBuilder visitParagraph(SamXParser.ParagraphContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      final int wrapLength = wrapParagraphAtColumn - indentLevel * indentString.length();

      builder.append(WordUtils.wrap(visitParagraphDirect(ctx).toString(), wrapLength));

      builder.append('\n');
      builder.append('\n');

      return builder;
   }

   @Override
   public StringBuilder visitTypedBlock(SamXParser.TypedBlockContext ctx)
   {
      StringBuilder builder = new StringBuilder();

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

      final List<SamXParser.BlockContext> blocks = ctx.block();
      visitNestedBlock(builder, blocks);

      return builder;
   }

   @Override
   public StringBuilder visitUnorderedList(SamXParser.UnorderedListContext ctx)
   {
      return visitList("*", ctx.listElement());
   }

   @Override
   public StringBuilder visitOrderedList(SamXParser.OrderedListContext ctx)
   {
      return visitList("#", ctx.listElement());
   }

   private StringBuilder visitList(String type, List<SamXParser.ListElementContext> elements)
   {
      StringBuilder builder = new StringBuilder();

      indentLevel++;

      for (SamXParser.ListElementContext lec : elements)
      {
         StringBuilder childBuilder = visitListElement(lec);
         if (childBuilder != null)
         {
            addIndent(builder);
            builder.append(type);
            builder.append(' ');
            builder.append(childBuilder);
         }
      }

      indentLevel--;

      return builder;
   }

   @Override
   public StringBuilder visitListElement(SamXParser.ListElementContext ctx)
   {
      final int wrapLength = wrapParagraphAtColumn - indentLevel * indentString.length() - 2;

      StringBuilder builder = new StringBuilder();

      {
         StringBuilder flowBuilder = new StringBuilder();

         final SamXParser.ConditionContext cond = ctx.condition();
         if (cond != null)
         {
            flowBuilder.append(visit(cond));
            flowBuilder.append(' ');
         }

         flowBuilder.append(visitFlow(ctx.flow()));

         final String wrappedFlow = WordUtils.wrap(flowBuilder.toString(), wrapLength);
         StringTokenizer tokenizer = new StringTokenizer(wrappedFlow, '\n');

         boolean firstLine = true;
         while (tokenizer.hasNext())
         {
            if (firstLine)
            {
               firstLine = false;
            }
            else
            {
               addIndent(builder);
               builder.append("  ");
            }
            builder.append(tokenizer.next());
            builder.append('\n');
         }
      }

      if (! ctx.paragraph().isEmpty())
      {
         for (SamXParser.ParagraphContext pc : ctx.paragraph())
         {
            StringBuilder paragraphBuilder = new StringBuilder();
            paragraphBuilder.append(visitParagraphDirect(pc));

            final String wrappedParagraphs = WordUtils.wrap(paragraphBuilder.toString(), wrapLength);
            StringTokenizer tokenizer = new StringTokenizer(wrappedParagraphs, '\n');

            while (tokenizer.hasNext())
            {
               addIndent(builder);
               builder.append("  ");
               builder.append(tokenizer.next());
               builder.append('\n');
            }
            builder.append('\n');
         }
      }
      else
      {
         builder.append('\n');
      }

      if (ctx.unorderedList() != null)
      {
         builder.append(visitUnorderedList(ctx.unorderedList()));
      }

      if (ctx.orderedList() != null)
      {
         builder.append(visitOrderedList(ctx.orderedList()));
      }

      return builder;
   }

   @Override
   public StringBuilder visitRecordSet(SamXParser.RecordSetContext ctx)
   {
      StringBuilder builder = new StringBuilder();

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

      int[] columnWidths = new int[ctx.headerRow().NAME().size() + 1];
      boolean[] isInteger = new boolean[ctx.headerRow().NAME().size()];

      ArrayList<String> headerElements = visitHeaderRowElements(ctx.headerRow());
      for (int ii = 0; ii < headerElements.size(); ++ii)
      {
         columnWidths[ii + 1] = headerElements.get(ii).length();
         isInteger[ii] = true;
      }

      ArrayList<ArrayList<String>> rows = new ArrayList<>(ctx.recordRow().size());

      for (SamXParser.RecordRowContext rrc : ctx.recordRow())
      {
         ArrayList<String> rowElements = visitRecordRowElements(rrc);
         rows.add(rowElements);

         {
            final int conditionLength = rowElements.get(0).length();
            if (columnWidths[0] < conditionLength)
            {
               columnWidths[0] = conditionLength;
            }
         }

         for (int ii = 0; ii < headerElements.size(); ++ii)
         {
            final String value = rowElements.get(ii + 1);
            final int length = value.length();
            if (columnWidths[ii + 1] < length)
            {
               columnWidths[ii + 1] = length;
            }

            if (!VisitorUtils.isInteger(value))
            {
               isInteger[ii] = false;
            }
         }
      }

      addIndent(builder);
      if (columnWidths[0] != 0)
      {
         builder.append(String.format("%1$-" + columnWidths[0] + "s", ""));
      }
      for (int ii = 0; ii < headerElements.size(); ++ii)
      {
         builder.append(" | ");
         if (ii != (headerElements.size() - 1))
         {
            builder.append(String.format("%1$-" + columnWidths[ii + 1] + "s", headerElements.get(ii)));
         }
         else
         {
            builder.append(headerElements.get(headerElements.size() - 1));
         }
      }
      builder.append('\n');

      for (ArrayList<String> rowData : rows)
      {
         addIndent(builder);
         if (columnWidths[0] != 0)
         {
            builder.append(String.format("%1$-" + columnWidths[0] + "s", rowData.get(0)));
         }
         for (int ii = 0; ii < headerElements.size(); ++ii)
         {
            builder.append(" | ");
            if (isInteger[ii])
            {
               builder.append(String.format("%1$" + columnWidths[ii + 1] + "s", rowData.get(ii + 1)));
            }
            else
            {
               if (ii != (headerElements.size() - 1))
               {
                  builder.append(String.format("%1$-" + columnWidths[ii + 1] + "s", rowData.get(ii + 1)));
               }
               else
               {
                  builder.append(rowData.get(headerElements.size()));
               }
            }
         }
         builder.append('\n');
      }

      indentLevel--;

      builder.append('\n');

      return builder;
   }

   @Override
   public StringBuilder visitHeaderRow(SamXParser.HeaderRowContext ctx)
   {
      StringBuilder builder = new StringBuilder();
      addIndent(builder);

      boolean firstToken = true;
      for (TerminalNode tc : ctx.NAME())
      {
         if (firstToken)
         {
            firstToken = false;
         }
         else
         {
            builder.append(' ');
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

      boolean firstToken = true;

      final SamXParser.ConditionContext cond = ctx.condition();
      if (cond != null)
      {
         builder.append(visit(cond));
         builder.append(' ');
         firstToken = false;
      }

      for (SamXParser.FlowContext tc : ctx.flow())
      {
         if (firstToken)
         {
            firstToken = false;
         }
         else
         {
            builder.append(' ');
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

   private ArrayList<String> visitHeaderRowElements(SamXParser.HeaderRowContext ctx)
   {
      ArrayList<String> result = new ArrayList<>(1 + ctx.NAME().size());

      for (TerminalNode tc : ctx.NAME())
      {
         result.add(tc.getText());
      }

      return result;
   }

   private ArrayList<String> visitRecordRowElements(SamXParser.RecordRowContext ctx)
   {
      ArrayList<String> result = new ArrayList<>(1 + ctx.flow().size());

      final SamXParser.ConditionContext cond = ctx.condition();
      if (cond != null)
      {
         result.add(visit(cond).toString());
      }
      else
      {
         result.add("");
      }

      for (SamXParser.FlowContext tc : ctx.flow())
      {
         StringBuilder childBuilder = visit(tc);
         if (childBuilder != null)
         {
            result.add(childBuilder.toString());
         }
         else
         {
            result.add("");
         }
      }

      return result;
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

         builder.append(tn.getText());
         firstToken = false;
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
   public StringBuilder visitIdentifierAttr(SamXParser.IdentifierAttrContext ctx)
   {
      return visitAttribute('*', ctx.NAME().getText());
   }

   @Override
   public StringBuilder visitLanguageAttr(SamXParser.LanguageAttrContext ctx)
   {
      return visitAttribute('!', ctx.NAME().getText());
   }

   @Override
   public StringBuilder visitCitationAttr(SamXParser.CitationAttrContext ctx)
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
      final SamXParser.ConditionContext cond = ctx.condition();
      if (cond != null)
      {
         builder.append(visit(cond));
      }
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

      for (SamXParser.DeclarationContext dc : ctx.declaration())
      {
         StringBuilder childBuilder = visit(dc);
         if (childBuilder != null)
         {
            builder.append(childBuilder);
         }
      }

      for (SamXParser.BlockContext bc : ctx.block())
      {
         indentLevel = 0;   // just in case
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

      final int codeBlockIndent = VisitorUtils.getTokenIndent(ctx, tokenStream) + indentLevel * indentString.length();

      builder.append("```(");
      builder.append(visit(ctx.language));
      builder.append(")");
      builder.append('\n');

      indentLevel++;
      for (SamXParser.ExternalCodeContext ecc : ctx.externalCode())
      {
         addIndent(builder);

         final int codeLineIndent = VisitorUtils.getTokenIndent(ecc, tokenStream);

         for (int ii = codeBlockIndent; ii < codeLineIndent; ++ii)
         {
            builder.append(' ');
         }

         builder.append(ecc.EXTCODE().getText());
         builder.append('\n');
      }
      indentLevel--;

      builder.append('\n');

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
      for (TerminalNode tn : ctx.NAME())
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

   public void setTokenStream(BufferedTokenStream tokens)
   {
      tokenStream = tokens;
   }

   @Override
   public StringBuilder visitConditionalBlock(SamXParser.ConditionalBlockContext ctx)
   {
      StringBuilder builder = new StringBuilder();
      builder.append(visit(ctx.condition()));
      builder.append('\n');

      final List<SamXParser.BlockContext> blocks = ctx.block();
      visitNestedBlock(builder, blocks);

      return builder;
   }

   private void visitNestedBlock(StringBuilder builder, List<SamXParser.BlockContext> blocks)
   {
      indentLevel++;
      for (SamXParser.BlockContext bc : blocks)
      {
         StringBuilder childBuilder = visit(bc);
         if (childBuilder != null)
         {
            addIndent(builder);
            builder.append(childBuilder);
         }
      }
      indentLevel--;
   }

   public void setLineWrapColumn(int column)
   {
      wrapParagraphAtColumn = column;
   }
}
