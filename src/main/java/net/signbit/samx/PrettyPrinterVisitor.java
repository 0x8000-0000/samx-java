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
import org.antlr.v4.runtime.ParserRuleContext;
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
   private final BufferedTokenStream tokenStream;

   private final PlainTextVisitor plainTextVisitor;

   private static final String indentString = "   ";
   private int wrapParagraphAtColumn = 72;

   public PrettyPrinterVisitor(BufferedTokenStream tokenStream)
   {
      this.tokenStream = tokenStream;
      plainTextVisitor = new PlainTextVisitor(tokenStream);
   }

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

      final String paragraphText = WordUtils.wrap(visitParagraphDirect(ctx).toString(), wrapLength);
      StringTokenizer tokenizer = new StringTokenizer(paragraphText, '\n');
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
         }
         builder.append(tokenizer.next());
         builder.append('\n');
      }

      builder.append('\n');

      return builder;
   }

   @Override
   public StringBuilder visitTypedBlock(SamXParser.TypedBlockContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      builder.append(ctx.NAME().getText());
      builder.append(ctx.TYPESEP().getText());
      renderConditionAndAttributes(ctx, builder);
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
         StringBuilder firstLineBuilder = new StringBuilder();

         final SamXParser.ConditionContext cond = ctx.condition();
         if (cond != null)
         {
            firstLineBuilder.append(visit(cond));
            firstLineBuilder.append(' ');
         }

         firstLineBuilder.append(visitFlow(ctx.flow()));

         if (ctx.skipped == null)
         {
            /* This means there is no empty line between the first and second lines in the
             * bulleted list; by the rules of paragraph handling, we have to join them.
             */

            if (!ctx.paragraph().isEmpty())
            {
               firstLineBuilder.append(' ');
               firstLineBuilder.append(visitParagraphDirect(ctx.paragraph(0)));
            }
         }

         final String wrappedFlow = WordUtils.wrap(firstLineBuilder.toString(), wrapLength);
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

      if (!ctx.paragraph().isEmpty())
      {
         boolean mergedFirstLine = false;
         if (ctx.skipped == null)
         {
            mergedFirstLine = true;
         }
         builder.append('\n');

         for (SamXParser.ParagraphContext pc : ctx.paragraph())
         {
            if (mergedFirstLine)
            {
               mergedFirstLine = false;
               continue;
            }

            final String paragraph = visitParagraphDirect(pc).toString();

            final String wrappedParagraphs = WordUtils.wrap(paragraph, wrapLength);
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
      renderConditionAndAttributes(ctx, builder);
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
         if (rrc.recordData() != null)
         {
            ArrayList<String> rowElements = visitRecordDataElements(rrc.recordData());
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

               if ((value != null) && (!value.isEmpty()))
               {
                  if (!VisitorUtils.isInteger(value))
                  {
                     isInteger[ii] = false;
                  }
               }
            }
         }

         if (rrc.recordSep() != null)
         {
            ArrayList<String> cols = new ArrayList<>();
            cols.add("+-");
            rows.add(cols);
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
         if (rowData.get(0).equals("+-"))
         {
            builder.append(' ');

            for (int jj = 0; jj < columnWidths[0]; ++ jj)
            {
               builder.append(' ');
            }

            for (int ii = 0; ii < headerElements.size(); ++ii)
            {
               builder.append("+---");
               for (int jj = 1; jj < columnWidths[ii + 1]; ++jj)
               {
                  builder.append('-');
               }
            }
         }
         else
         {
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

   private ArrayList<String> visitHeaderRowElements(SamXParser.HeaderRowContext ctx)
   {
      ArrayList<String> result = new ArrayList<>(1 + ctx.NAME().size());

      for (TerminalNode tc : ctx.NAME())
      {
         result.add(tc.getText());
      }

      return result;
   }

   private ArrayList<String> visitRecordDataElements(SamXParser.RecordDataContext ctx)
   {
      ArrayList<String> result = new ArrayList<>(1 + ctx.optionalFlow().size());

      final SamXParser.ConditionContext cond = ctx.condition();
      if (cond != null)
      {
         result.add(visit(cond).toString());
      }
      else
      {
         result.add("");
      }

      for (SamXParser.OptionalFlowContext tc : ctx.optionalFlow())
      {
         if (tc.flow() != null)
         {
            StringBuilder childBuilder = visitFlow(tc.flow());
            if (childBuilder != null)
            {
               result.add(childBuilder.toString());
            }
            else
            {
               result.add("");
            }
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
      return plainTextVisitor.visitText(ctx);
   }

   @Override
   public StringBuilder visitFlow(SamXParser.FlowContext ctx)
   {
      StringBuilder builder = new StringBuilder();
      boolean firstToken = true;
      for (ParseTree tn : ctx.children)
      {
         StringBuilder childBuilder = visit(tn);
         if (childBuilder != null)
         {
            if (!firstToken)
            {
               addSpaceIfPresentInInput(builder, tn);
            }

            builder.append(childBuilder);
            firstToken = false;
         }
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

   @Override
   public StringBuilder visitPhrase(SamXParser.PhraseContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      builder.append('{');
      builder.append(visit(ctx.text()));
      builder.append('}');
      renderConditionAndAttributes(ctx, builder);
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
   public StringBuilder visitClassAttr(SamXParser.ClassAttrContext ctx)
   {
      return visitAttribute('.', ctx.NAME().getText());
   }

   @Override
   public StringBuilder visitIdentifierAttr(SamXParser.IdentifierAttrContext ctx)
   {
      return visitAttribute('#', ctx.NAME().getText());
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
   public StringBuilder visitReferenceAttr(SamXParser.ReferenceAttrContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      builder.append("[*");
      builder.append(ctx.NAME().getText());
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
      renderConditionAndAttributes(ctx, builder);
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

   @Override
   public StringBuilder visitConditionalBlock(SamXParser.ConditionalBlockContext ctx)
   {
      StringBuilder builder = new StringBuilder();
      builder.append(visit(ctx.condition()));
      builder.append('\n');
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

   @Override
   public StringBuilder visitDefineFragment(SamXParser.DefineFragmentContext ctx)
   {
      StringBuilder builder = new StringBuilder();
      builder.append("~~~(*");
      builder.append(ctx.name.getText());
      builder.append(")");
      renderConditionAndAttributes(ctx, builder);
      builder.append('\n');
      builder.append('\n');

      visitNestedBlock(builder, ctx.block());

      return builder;
   }

   @Override
   public StringBuilder visitInsertFragment(SamXParser.InsertFragmentContext ctx)
   {
      StringBuilder builder = new StringBuilder();
      builder.append(">>>(*");
      builder.append(ctx.name.getText());
      builder.append(')');
      renderConditionAndAttributes(ctx, builder);
      builder.append('\n');
      builder.append('\n');

      return builder;
   }

   @Override
   public StringBuilder visitInsertImage(SamXParser.InsertImageContext ctx)
   {
      StringBuilder builder = new StringBuilder();
      builder.append(">>>(image ");
      builder.append(visitText(ctx.text()));
      builder.append(')');
      renderConditionAndAttributes(ctx, builder);
      if (ctx.description != null)
      {
         builder.append(' ');
         builder.append(visitFlow(ctx.description));
      }
      builder.append('\n');
      builder.append('\n');

      return builder;
   }

   @Override
   public StringBuilder visitLocalInsert(SamXParser.LocalInsertContext ctx)
   {
      StringBuilder builder = new StringBuilder();
      builder.append(">($");
      builder.append(visitText(ctx.text()));
      builder.append(')');
      return builder;
   }


   private String renderGridElement(SamXParser.GridElementContext gec)
   {
      final SamXParser.FlowContext fc = gec.optionalFlow().flow();
      if (fc == null)
      {
         return "";
      }
      else
      {
         return visitFlow(fc).toString();
      }
   }

   private ArrayList<String> renderGridElementList(SamXParser.ConditionContext condition, List<SamXParser.AttributeContext> attributes, List<SamXParser.GridElementContext> elements)
   {
      ArrayList<String> result = new ArrayList<>(1 + elements.size());

      StringBuilder builder = new StringBuilder();
      if (condition != null)
      {
         builder.append(visit(condition));
      }
      for (SamXParser.AttributeContext ac : attributes)
      {
         builder.append(visit(ac));
      }
      result.add(builder.toString());

      for (SamXParser.GridElementContext gec : elements)
      {
         result.add(renderGridElement(gec));
      }

      return result;
   }

   private void renderConditionAndAttributes(ParserRuleContext prc, StringBuilder builder)
   {
      final SamXParser.ConditionContext cond = prc.getRuleContext(SamXParser.ConditionContext.class, 0);
      if (cond != null)
      {
         builder.append(visit(cond));
      }
      for (SamXParser.AttributeContext ac : prc.getRuleContexts(SamXParser.AttributeContext.class))
      {
         builder.append(visit(ac));
      }
   }

   @Override
   public StringBuilder visitGrid(SamXParser.GridContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      builder.append("+++");
      renderConditionAndAttributes(ctx, builder);
      if (ctx.description != null)
      {
         builder.append(' ');
         builder.append(visit(ctx.description));
      }
      builder.append('\n');
      builder.append('\n');

      indentLevel++;

      int[] columnWidths = new int[ctx.gridHeaderRow().gridElement().size() + 1];
      boolean[] isInteger = new boolean[ctx.gridHeaderRow().gridElement().size() + 1];

      final SamXParser.GridHeaderRowContext ghrc = ctx.gridHeaderRow();
      ArrayList<String> headerElements = renderGridElementList(null, ghrc.attribute(), ghrc.gridElement());
      for (int ii = 0; ii < headerElements.size(); ++ii)
      {
         columnWidths[ii] = headerElements.get(ii).length();
         isInteger[ii] = true;
      }

      ArrayList<ArrayList<String>> rows = new ArrayList<>(ctx.gridRecordRow().size());

      for (SamXParser.GridRecordRowContext rrc : ctx.gridRecordRow())
      {
         ArrayList<String> rowElements = renderGridElementList(rrc.condition(), rrc.attribute(), rrc.gridElement());
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
            final String value = rowElements.get(ii);
            final int length = value.length();
            if (columnWidths[ii] < length)
            {
               columnWidths[ii] = length;
            }

            if ((value != null) && (!value.isEmpty()))
            {
               if (!VisitorUtils.isInteger(value))
               {
                  isInteger[ii] = false;
               }
            }
         }
      }

      addIndent(builder);
      if (columnWidths[0] != 0)
      {
         builder.append(String.format("%1$-" + columnWidths[0] + "s", headerElements.get(0)));
      }
      for (int ii = 1; ii < headerElements.size(); ++ii)
      {
         builder.append(" | ");
         if (ii != (headerElements.size() - 1))
         {
            builder.append(String.format("%1$-" + columnWidths[ii] + "s", headerElements.get(ii)));
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
         for (int ii = 1; ii < headerElements.size(); ++ii)
         {
            builder.append(" | ");
            if (isInteger[ii])
            {
               builder.append(String.format("%1$" + columnWidths[ii] + "s", rowData.get(ii)));
            }
            else
            {
               if (ii != (headerElements.size() - 1))
               {
                  builder.append(String.format("%1$-" + columnWidths[ii] + "s", rowData.get(ii)));
               }
               else
               {
                  builder.append(rowData.get(headerElements.size() - 1));
               }
            }
         }
         builder.append('\n');
      }

      indentLevel--;

      builder.append('\n');

      return builder;
   }
}
