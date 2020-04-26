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
      for (int ii = 0; ii < indentLevel; ++ ii)
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

   private StringBuilder wrapText(String rawText, int wrapLength, boolean indentFirst)
   {
      StringBuilder builder = new StringBuilder();
      final String paragraphText = WordUtils.wrap(rawText, wrapLength);
      StringTokenizer tokenizer = new StringTokenizer(paragraphText, '\n');
      while (tokenizer.hasNext())
      {
         if (! indentFirst)
         {
            indentFirst = true;
         }
         else
         {
            addIndent(builder);
         }
         builder.append(tokenizer.next());
         builder.append('\n');
      }

      return builder;
   }

   @Override
   public StringBuilder visitParagraph(SamXParser.ParagraphContext ctx)
   {
      final String rawText = visitParagraphDirect(ctx).toString();

      final int wrapLength = wrapParagraphAtColumn - indentLevel * indentString.length();

      return wrapText(rawText, wrapLength, true).append('\n');
   }

   @Override
   public StringBuilder visitTypedBlock(SamXParser.TypedBlockContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      addIndent(builder);
      builder.append(ctx.NAME().getText());
      builder.append(ctx.TYPESEP().getText());
      builder.append(visitBlockMetadata(ctx.blockMetadata()));
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

      for (SamXParser.ListElementContext lec : elements)
      {
         addIndent(builder);
         StringBuilder childBuilder = visitListElement(lec);
         if (childBuilder != null)
         {
            builder.append(type);
            builder.append(' ');
            builder.append(childBuilder);
         }
      }

      return builder;
   }

   @Override
   public StringBuilder visitListElement(SamXParser.ListElementContext ctx)
   {
      StringBuilder firstLineFlow = new StringBuilder();

      final SamXParser.ConditionContext cond = ctx.metadata().condition();
      if (cond != null)
      {
         firstLineFlow.append(visit(cond));
         firstLineFlow.append(' ');
      }

      firstLineFlow.append(visitFlow(ctx.flow()));

      ArrayList<SamXParser.BlockContext> blocks = new ArrayList<>(ctx.block());

      boolean joined = false;
      if (! blocks.isEmpty())
      {
         if (ctx.separator == null)
         {
            SamXParser.BlockContext firstBlock = blocks.get(0);
            if (firstBlock.getChildCount() == 1)
            {
               SamXParser.ParagraphContext pc = firstBlock.getChild(SamXParser.ParagraphContext.class, 0);

               if (pc != null)
               {
                  // need to join the first paragraph with the flow

                  firstLineFlow.append(' ');
                  firstLineFlow.append(visitParagraphDirect(pc));
                  joined = true;

                  blocks.remove(0);
               }
            }
         }
      }

      StringBuilder builder = new StringBuilder();

      indentLevel++;
      final int wrapLength = wrapParagraphAtColumn - indentLevel * indentString.length();
      builder.append(wrapText(firstLineFlow.toString(), wrapLength, false));
      indentLevel--;

      if (! blocks.isEmpty())
      {
         if ((ctx.separator != null) || joined)
         {
            builder.append('\n');
         }

         visitNestedBlock(builder, blocks);
      }

      final int builderLen = builder.length();
      if (builderLen > 2)
      {
         if ((builder.charAt(builderLen - 1) != '\n') || (builder.charAt(builderLen - 2) != '\n'))
         {
            builder.append('\n');
         }
      }

      return builder;
   }

   @Override
   public StringBuilder visitBlockMetadata(SamXParser.BlockMetadataContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      renderConditionAndAttributes(ctx, builder);
      if (ctx.description != null)
      {
         builder.append(' ');
         builder.append(visit(ctx.description));
      }
      builder.append('\n');

      return builder;
   }

   @Override
   public StringBuilder visitRecordSet(SamXParser.RecordSetContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      addIndent(builder);
      builder.append(ctx.NAME().getText());
      builder.append(ctx.RECSEP().getText());
      builder.append(visitBlockMetadata(ctx.blockMetadata()));
      builder.append('\n');

      indentLevel++;

      int[] columnWidths = new int[ctx.headerRow().NAME().size() + 1];
      boolean[] isInteger = new boolean[ctx.headerRow().NAME().size()];

      ArrayList<String> headerElements = visitHeaderRowElements(ctx.headerRow());
      for (int ii = 0; ii < headerElements.size(); ++ ii)
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

            for (int ii = 0; ii < headerElements.size(); ++ ii)
            {
               final String value = rowElements.get(ii + 1);
               final int length = value.length();
               if (columnWidths[ii + 1] < length)
               {
                  columnWidths[ii + 1] = length;
               }

               if ((value != null) && (! value.isEmpty()))
               {
                  if (! VisitorUtils.isInteger(value))
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
      for (int ii = 0; ii < headerElements.size(); ++ ii)
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

            for (int ii = 0; ii < headerElements.size(); ++ ii)
            {
               builder.append("+---");
               for (int jj = 1; jj < columnWidths[ii + 1]; ++ jj)
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
            for (int ii = 0; ii < headerElements.size(); ++ ii)
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
            if (! firstToken)
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
         if ((precedingTokens != null) && (! precedingTokens.isEmpty()))
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
      builder.append(visitFlow(ctx.flow()));
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

      addIndent(builder);

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

         for (int ii = codeBlockIndent; ii < codeLineIndent; ++ ii)
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

      addIndent(builder);
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
      boolean lastChildHadNewline = false;
      for (SamXParser.BlockContext bc : blocks)
      {
         StringBuilder childBuilder = visit(bc);
         if (childBuilder != null)
         {
            builder.append(childBuilder);

            // hack to add empty lines between top-level blocks
            final int childLen = childBuilder.length();
            if (childLen > 2)
            {
               if ((childBuilder.charAt(childLen - 1) == '\n') && (childBuilder.charAt(childLen - 2) == '\n'))
               {
                  lastChildHadNewline = true;
               }
            }
            //builder.append(String.format("NB: %d\n", indentLevel));
         }
      }

      if (! lastChildHadNewline)
      {
         builder.append('\n');
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

      addIndent(builder);
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

      addIndent(builder);
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

      addIndent(builder);
      builder.append(">>>(image ");
      builder.append(visitText(ctx.text()));
      builder.append(')');
      builder.append(visitBlockMetadata(ctx.blockMetadata()));
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
         StringBuilder builder = new StringBuilder();

         for (SamXParser.AttributeContext ac : gec.attribute())
         {
            builder.append(visit(ac));
         }

         if (! gec.attribute().isEmpty())
         {
            builder.append(' ');
         }

         builder.append(visitFlow(fc));

         return builder.toString();
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
      final SamXParser.MetadataContext metadata = prc.getRuleContext(SamXParser.MetadataContext.class, 0);
      builder.append(visitMetadata(metadata));
   }

   @Override
   public StringBuilder visitMetadata(SamXParser.MetadataContext mc)
   {
      StringBuilder builder = new StringBuilder();
      final SamXParser.ConditionContext cond = mc.condition();
      if (cond != null)
      {
         builder.append(visit(cond));
      }
      for (SamXParser.AttributeContext ac : mc.attribute())
      {
         builder.append(visit(ac));
      }
      return builder;
   }

   @Override
   public StringBuilder visitGeneralGrid(SamXParser.GeneralGridContext ctx)
   {
      final GridVisitor.GeneralGridGroup body = new GridVisitor.GeneralGridGroup(ctx.body, this);

      int conditionColumnWidth = body.conditionColumnWidth;
      int columnWidths[] = new int[body.columnCount];
      for (int ii = 0; ii < body.columnCount; ++ ii)
      {
         columnWidths[ii] = body.columnWidths[ii];
      }

      GridVisitor.GeneralGridGroup header = null;
      if (ctx.header != null)
      {
         header = new GridVisitor.GeneralGridGroup(ctx.header, this);
         if (header.columnCount != body.columnCount)
         {
            throw new RuntimeException(String.format("Invalid table specification: multiple table column sizes between header (%d) and body (%d)", header.columnWidths.length, body.columnWidths.length));
         }

         if (conditionColumnWidth < header.conditionColumnWidth)
         {
            conditionColumnWidth = header.conditionColumnWidth;
         }

         for (int ii = 0; ii < body.columnCount; ++ ii)
         {
            if (columnWidths[ii] < header.columnWidths[ii])
            {
               columnWidths[ii] = header.columnWidths[ii];
            }
         }
      }

      GridVisitor.GeneralGridGroup footer = null;
      if (ctx.footer != null)
      {
         footer = new GridVisitor.GeneralGridGroup(ctx.footer, this);
         if (footer.columnCount != body.columnCount)
         {
            throw new RuntimeException(String.format("Invalid table specification: multiple table column sizes between footer (%d) and body (%d)", footer.columnWidths.length, body.columnWidths.length));
         }

         if (conditionColumnWidth < footer.conditionColumnWidth)
         {
            conditionColumnWidth = footer.conditionColumnWidth;
         }

         for (int ii = 0; ii < body.columnCount; ++ ii)
         {
            if (columnWidths[ii] < footer.columnWidths[ii])
            {
               columnWidths[ii] = footer.columnWidths[ii];
            }
         }
      }

      StringBuilder builder = new StringBuilder();

      builder.append("+++");
      builder.append(visitBlockMetadata(ctx.blockMetadata()));
      builder.append('\n');

      indentLevel++;

      if (header != null)
      {
         renderGeneralTableGroup(conditionColumnWidth, columnWidths, header, builder);

         renderGeneralTableSeparator(conditionColumnWidth, columnWidths, header.columnCount, builder);
      }

      renderGeneralTableGroup(conditionColumnWidth, columnWidths, body, builder);

      if (footer != null)
      {
         renderGeneralTableSeparator(conditionColumnWidth, columnWidths, footer.columnCount, builder);

         renderGeneralTableGroup(conditionColumnWidth, columnWidths, footer, builder);
      }

      indentLevel--;

      builder.append('\n');

      return builder;
   }

   private void renderGeneralTableSeparator(int conditionColumnWidth, int[] columnWidths, int columnCount, StringBuilder builder)
   {
      addIndent(builder);
      for (int jj = 0; jj < conditionColumnWidth + 1; ++ jj)
      {
         builder.append(' ');
      }
      for (int jj = 0; jj < columnCount; ++ jj)
      {
         builder.append('+');
         for (int kk = 0; kk < columnWidths[jj] + 2; kk++)
         {
            builder.append('=');
         }
      }

      builder.append("+\n");
   }

   private void renderGeneralTableGroup(int conditionColumnWidth, int[] columnWidths, GridVisitor.GeneralGridGroup gridGroup, StringBuilder builder)
   {
      for (GridVisitor.GeneralGridRow ggr : gridGroup.rows)
      {
         addIndent(builder);
         if (conditionColumnWidth > 0)
         {
            final String attributeCondition = visitMetadata(ggr.metadataContext).toString();
            builder.append(String.format("%1$-" + conditionColumnWidth + "s", attributeCondition));
         }

         for (int jj = 0; jj < gridGroup.columnCount; ++ jj)
         {
            final GridVisitor.GridCell gc = ggr.cells.get(jj);
            if (gc.colSpan > 0)
            {
               builder.append(" |");
               int columnWidth = columnWidths[jj];
               final int colSpan = gc.colSpan;
               for (int kk = 1; kk < colSpan; ++ kk)
               {
                  columnWidth += columnWidths[jj + kk] + 2;
                  ggr.cells.get(jj + kk).colSpan = 0;
                  builder.append('|');
               }
               if (gc.rowSpan > 1)
               {
                  builder.append('-');
                  columnWidth--;
                  for (int kk = 1; kk < gc.rowSpan; ++ kk)
                  {
                     builder.append('-');
                     columnWidth--;
                  }
               }
               final String attributes = gc.getAttributesPlain();
               builder.append(attributes);
               builder.append(' ');
               columnWidth -= attributes.length();

               final String cellContent = gc.getContent(PrettyPrinterVisitor.this);

               if (gridGroup.isInteger[jj] || gridGroup.isCurrency[jj])
               {
                  builder.append(String.format("%1$" + columnWidth + "s", cellContent));
               }
               else
               {
                  builder.append(String.format("%1$-" + columnWidth + "s", cellContent));
               }
            }
         }

         builder.append(" |\n");
      }
   }

   @Override
   public StringBuilder visitIncludeFile(SamXParser.IncludeFileContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      addIndent(builder);
      final String reference = ctx.reference.getText();
      builder.append("<<<(");
      builder.append(reference);
      builder.append(")\n");

      return builder;
   }
}
