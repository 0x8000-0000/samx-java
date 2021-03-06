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

package net.signbit.samx.visitors;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import net.signbit.samx.Parser;
import net.signbit.samx.parser.SamXLexer;
import net.signbit.samx.parser.SamXParser;

public class XmlTextVisitor extends RendererVisitor
{
   private boolean indentParagraph = true;
   private boolean writeXmlDeclaration = true;
   private boolean forceWrapElement = false;

   private String topElement = "document";
   private String topElementNamespace = "https://mbakeranalecta.github.io/sam/";
   private String topElementVersion = null;

   private boolean docBookMode = false;
   private boolean ditaMode = false;

   public XmlTextVisitor(Writer aWriter, HashMap<String, Parser.Result> docDict, HashMap<String, IOException> errDict, HashMap<String, String> referenceDict, BufferedTokenStream tokenStream)
   {
      super(aWriter, docDict, errDict, referenceDict, tokenStream);
   }

   public void skipXmlDeclaration()
   {
      writeXmlDeclaration = false;
   }

   public void setIndentLevel(int newLevel)
   {
      indentLevel = newLevel;
   }

   public void setTopElement(String name)
   {
      topElement = name;
      forceWrapElement = true;
   }

   public void setTopElementNamespace(String name)
   {
      topElementNamespace = name;
      forceWrapElement = true;
   }

   public void setTopElementVersion(String name)
   {
      topElementVersion = name;
   }


   @Override
   public Exception visitDocument(SamXParser.DocumentContext ctx)
   {
      if (writeXmlDeclaration)
      {
         append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
      }

      if ((ctx.block().size() > 1) || (forceWrapElement))
      {
         if (writeXmlDeclaration)
         {
            append('<');
            append(topElement);

            if (topElementNamespace != null)
            {
               append(" xmlns=\"");
               append(topElementNamespace);
               append('"');

               if (docBookMode)
               {
                  append(" xmlns:xl=\"http://www.w3.org/1999/xlink\"");
               }

               if (topElementVersion != null)
               {
                  append(" version=\"");
                  append(topElementVersion);
                  append('"');
               }
            }

            append('>');
            appendNewline();

            indentLevel++;
         }

         for (SamXParser.BlockContext bc : ctx.block())
         {
            visit(bc);
         }

         if (writeXmlDeclaration)
         {
            indentLevel--;

            append("</");
            append(topElement);
            append('>');
            appendNewline();
         }
      }
      else
      {
         visit(ctx.block(0));
      }

      return exception;
   }

   @Override
   public Exception visitField(SamXParser.FieldContext ctx)
   {
      addIndent();

      StringBuilder openTag = new StringBuilder();
      openTag.append('<');
      openTag.append(ctx.NAME().getText());
      openTag.append('>');
      append(openTag);

      visit(ctx.flow());

      StringBuilder closeTag = new StringBuilder();
      closeTag.append('<');
      closeTag.append('/');
      closeTag.append(ctx.NAME().getText());
      closeTag.append('>');

      append(closeTag);
      appendNewline();

      return null;
   }

   private void addSpaceIfPresentInInput(ParseTree tn)
   {
      final Interval pos = tn.getSourceInterval();
      if (pos.a <= pos.b)
      {
         final List<Token> precedingTokens = tokenStream.getHiddenTokensToLeft(pos.a, SamXLexer.WHITESPACE);
         if ((precedingTokens != null) && (! precedingTokens.isEmpty()))
         {
            append(' ');
         }
      }
   }

   @Override
   public Object visitFlow(SamXParser.FlowContext ctx)
   {
      boolean firstElement = true;

      for (ParseTree pt : ctx.children)
      {
         if (! firstElement)
         {
            addSpaceIfPresentInInput(pt);
         }
         firstElement = false;

         visit(pt);
      }

      return null;
   }

   @Override
   public Object visitPhrase(SamXParser.PhraseContext ctx)
   {
      if (isDisabled(ctx))
      {
         return null;
      }

      if (! ctx.annotation().isEmpty())
      {
         final List<SamXParser.UrlContext> url = ctx.annotation(0).flow().url();
         if ((url != null) && (! url.isEmpty()))
         {
            if (docBookMode)
            {
               append("<link xl:href=\"");
               visitUrl(url.get(0));
               append("\">");
               visit(ctx.text());
               append("</link>");
            }
         }
      }
      else
      {
         renderElementWithAttributes("phrase", ctx.metadata().attribute());
         visit(ctx.text());
         append("</phrase>");
      }

      return null;
   }

   @Override
   public Exception visitText(SamXParser.TextContext ctx)
   {
      boolean firstToken = true;

      for (ParseTree pt : ctx.children)
      {
         if (! firstToken)
         {
            final Interval pos = pt.getSourceInterval();
            if (pos.a <= pos.b)
            {
               final List<Token> precedingWhitespace = tokenStream.getHiddenTokensToLeft(pos.a, SamXLexer.WHITESPACE);
               if ((precedingWhitespace != null) && (! precedingWhitespace.isEmpty()))
               {
                  append(' ');
               }
            }
         }

         //final String text = pt.getText();
         //append(text);
         visit(pt);
         firstToken = false;
      }

      return null;
   }

   @Override
   public Exception visitEmpty(SamXParser.EmptyContext ctx)
   {
      return null;
   }

   @Override
   public Object visitTypedBlock(SamXParser.TypedBlockContext ctx)
   {
      if (isDisabled(ctx))
      {
         return null;
      }

      final String typeText = ctx.NAME().getText();
      addIndent();
      append('<');
      append(typeText);
      append('>');
      appendNewline();

      indentLevel++;

      renderTitle(ctx);

      for (SamXParser.BlockContext pt : ctx.block())
      {
         visit(pt);
      }

      indentLevel--;

      appendCloseTag(typeText);

      return null;
   }

   private void appendCloseTag(String typeText)
   {
      addIndent();
      append('<');
      append('/');
      append(typeText);
      append('>');
      appendNewline();
   }

   @Override
   public Exception visitParagraph(SamXParser.ParagraphContext ctx)
   {
      if (indentParagraph)
      {
         addIndent();
      }

      append('<');
      append(getParagraphTag());
      append('>');

      visitParagraphContents(ctx);

      append('<');
      append('/');
      append(getParagraphTag());
      append('>');

      if (indentParagraph)
      {
         appendNewline();
      }

      return null;
   }

   private void visitParagraphContents(SamXParser.ParagraphContext ctx)
   {
      int offset = charactersWritten;

      for (SamXParser.FlowContext fc : ctx.flow())
      {
         if (offset != charactersWritten)
         {
            append(' ');
            offset = charactersWritten;
         }

         visit(fc);
      }
   }

   private void visitGenericList(String tagType, List<SamXParser.ListElementContext> elements)
   {
      addIndent();
      append('<');
      append(tagType);
      append('>');
      appendNewline();

      indentLevel++;

      final boolean saveIndentParagraph = indentParagraph;
      indentParagraph = false;

      for (SamXParser.ListElementContext lec : elements)
      {
         if ((lec.metadata().condition() == null) || Boolean.TRUE.equals(visit(lec.metadata().condition())))
         {
            addIndent();

            append('<');
            append(getListItemTag());
            append("><");
            append(getParagraphTag());
            append('>');
            visit(lec.flow());

            ArrayList<SamXParser.BlockContext> blocks = new ArrayList<>(lec.block());
            if (! blocks.isEmpty())
            {
               if (lec.separator == null)
               {
                  SamXParser.BlockContext firstBlock = blocks.get(0);
                  if (firstBlock.getChildCount() == 1)
                  {
                     SamXParser.ParagraphContext pc = firstBlock.getChild(SamXParser.ParagraphContext.class, 0);

                     if (pc != null)
                     {
                        // need to join the first paragraph with the flow

                        append(' ');
                        visitParagraphContents(pc);
                        blocks.remove(0);
                     }
                  }
               }
            }

            append("</");
            append(getParagraphTag());
            append('>');

            if (! blocks.isEmpty())
            {
               appendNewline();
            }

            indentParagraph = true;
            indentLevel++;
            for (SamXParser.BlockContext bc : blocks)
            {
               visit(bc);
               //appendNewline();
            }
            indentLevel--;

            if (! blocks.isEmpty())
            {
               addIndent();
            }
            append("</");
            append(getListItemTag());
            append('>');

            appendNewline();
         }
      }

      indentParagraph = saveIndentParagraph;

      indentLevel--;

      addIndent();
      append("</");
      append(tagType);
      append('>');
      appendNewline();
   }

   @Override
   public Exception visitUnorderedList(SamXParser.UnorderedListContext ctx)
   {
      visitGenericList(getUnorderedListTag(), ctx.listElement());

      return null;
   }

   @Override
   public Object visitOrderedList(SamXParser.OrderedListContext ctx)
   {
      visitGenericList(getOrderedListTag(), ctx.listElement());

      return null;
   }

   private Object visitRecordSetNative(SamXParser.RecordSetContext ctx)
   {
      final String typeText = ctx.NAME().getText();

      addIndent();
      append('<');
      append(typeText);
      if (ctx.blockMetadata().metadata().attribute().size() > 0)
      {
         append(' ');
         for (SamXParser.AttributeContext ac : ctx.blockMetadata().metadata().attribute())
         {
            visit(ac);
         }
      }
      append('>');
      appendNewline();

      indentLevel++;

      final SamXParser.HeaderRowContext header = ctx.headerRow();

      SamXParser.RecordDataContext lastRecordData = null;

      for (SamXParser.RecordRowContext rrc : ctx.recordRow())
      {
         final SamXParser.RecordDataContext rdc = rrc.recordData();
         if (rdc != null)
         {
            if (isDisabled(rdc))
            {
               continue;
            }

            addIndent();
            append("<record>");
            appendNewline();

            indentLevel++;

            for (int ii = 0; ii < header.NAME().size(); ii++)
            {
               addIndent();
               append('<');
               append(header.NAME(ii).getText());

               final SamXParser.FlowContext thisFlow = rdc.optionalFlow(ii).flow();
               if (thisFlow != null)
               {
                  append('>');
                  visitFlow(thisFlow);
                  append('<');
                  append('/');
                  append(header.NAME(ii).getText());
               }
               else
               {
                  if (lastRecordData != null)
                  {
                     final SamXParser.FlowContext previousFlow = lastRecordData.optionalFlow(ii).flow();
                     if (previousFlow != null)
                     {
                        append('>');
                        visitFlow(previousFlow);
                        append('<');
                        append('/');
                        append(header.NAME(ii).getText());
                     }
                     else
                     {
                        append('/');
                     }
                  }
                  else
                  {
                     append('/');
                  }
               }

               append('>');
               appendNewline();
            }

            indentLevel--;

            appendCloseTag("record");

            lastRecordData = rdc;
         }

         if (rrc.recordSep() != null)
         {
            lastRecordData = null;
         }
      }

      indentLevel--;

      appendCloseTag(typeText);

      return null;
   }

   private Object visitRecordSetDocBook(SamXParser.RecordSetContext ctx)
   {
      RecordSetVisitor visitor = new RecordSetVisitor(tokenStream);
      RecordSetVisitor.RecordSet rs = (RecordSetVisitor.RecordSet) visitor.visitRecordSet(ctx);

      rs.computePresentation(new PlainTextVisitor(tokenStream));

      addIndent();
      ArrayList<String> extraClass = new ArrayList<>(1);
      extraClass.add(ctx.NAME().toString());
      renderElementWithAttributes("table", ctx.blockMetadata().metadata().attribute(), extraClass);
      appendNewline();

      indentLevel++;

      renderTitle(ctx);

      final int columnCount = rs.header.attributes.size();

      addIndent();
      append(String.format("<tgroup cols=\"%d\">", columnCount));
      appendNewline();

      for (int ii = 1; ii <= columnCount; ++ ii)
      {
         addIndent();
         append(String.format("<colspec colname=\"c%d\"/>", ii));
         appendNewline();
      }

      /*
       * header
       */
      addIndent();
      append("<thead>\n");
      indentLevel++;
      addIndent();
      append("<row>\n");

      indentLevel++;
      for (String column : rs.header.attributes)
      {
         addIndent();
         append("<entry>");
         append(column);
         append("</entry>\n");
      }
      indentLevel--;

      addIndent();
      append("</row>\n");
      indentLevel--;
      addIndent();
      append("</thead>\n");

      /*
       * body
       */
      addIndent();
      append("<tbody>\n");
      indentLevel++;

      for (RecordSetVisitor.RecordDataGroup rdg : rs.groups)
      {
         addIndent();
         append("<!-- group -->\n");

         boolean firstRow = true;

         for (RecordSetVisitor.RecordData rd : rdg.rows)
         {
            addIndent();
            append("<row>\n");

            indentLevel++;
            for (int ii = 0; ii < rd.flows.size(); ++ ii)
            {
               SamXParser.FlowContext fc = rd.flows.get(ii);
               if ((fc == null) && ((ii + 1) == rd.flows.size()))
               {
                  continue;
               }

               if (rdg.hasSingleValue(ii))
               {
                  if (firstRow)
                  {
                     addIndent();

                     String alignRight = "";
                     if (rs.isInteger[ii] && (fc != null))
                     {
                        alignRight = " align=\"right\"";
                     }

                     String moreRows = "";
                     if (rdg.rows.size() > 1)
                     {
                        moreRows = String.format(" valign=\"top\" morerows=\"%d\"", rdg.rows.size() - 1);
                     }

                     append(String.format("<entry%s%s>", moreRows, alignRight));
                     if (fc != null)
                     {
                        visitFlow(fc);
                     }
                     append("</entry>\n");
                  }
               }
               else
               {
                  addIndent();
                  if (fc != null)
                  {
                     String alignRight = "";
                     if (rs.isInteger[ii])
                     {
                        alignRight = " align=\"right\"";
                     }

                     append(String.format("<entry%s>", alignRight));
                     visitFlow(fc);
                     append("</entry>\n");
                  }
                  else
                  {
                     append("<entry/>\n");
                  }
               }
            }
            indentLevel--;

            addIndent();
            append("</row>\n");

            firstRow = false;
         }
      }

      indentLevel--;
      addIndent();
      append("</tbody>\n");


      indentLevel--;
      addIndent();
      append("</tgroup>");
      appendNewline();

      indentLevel--;

      appendCloseTag("table");

      return null;
   }

   @Override
   public Object visitRecordSet(SamXParser.RecordSetContext ctx)
   {
      if (isDisabled(ctx))
      {
         return null;
      }

      if (docBookMode)
      {
         return visitRecordSetDocBook(ctx);
      }
      else
      {
         return visitRecordSetNative(ctx);
      }
   }

   @Override
   public Exception visitIncludeFile(SamXParser.IncludeFileContext ctx)
   {
      if (isDisabled(ctx))
      {
         return null;
      }

      StringBuilder builder = new StringBuilder();

      builder.append("<!-- ");
      builder.append("begin include: ");

      final String reference = ctx.reference.getText();

      builder.append(reference);

      String absolutePath = referencePaths.get(reference);

      Parser.Result includedResult = includedDocuments.get(absolutePath);
      if (includedResult == null)
      {
         builder.append(" is not found: ");

         IOException ioe = includedExceptions.get(absolutePath);

         if (ioe != null)
         {
            builder.append(ioe.getMessage());
         }
         else
         {
            builder.append(" exception missing");
         }
      }

      builder.append(" -->");

      append(builder);
      appendNewline();

      if (includedResult != null)
      {
         XmlTextVisitor visitor = new XmlTextVisitor(writer, includedDocuments, includedExceptions, includedResult.referencePaths, includedResult.tokens);
         visitor.skipXmlDeclaration();
         visitor.setIndentLevel(indentLevel + 1);
         if (docBookMode)
         {
            visitor.setDocBookMode();
         }
         if (ditaMode)
         {
            visitor.setDitaMode();
         }

         visitor.visit(includedResult.document);

         StringBuilder endBuilder = new StringBuilder();
         endBuilder.append("<!-- ");
         endBuilder.append("end include: ");
         endBuilder.append(reference);
         endBuilder.append(" -->");
         append(endBuilder);
         appendNewline();
      }

      return null;
   }

   @Override
   public HashSet<String> visitNameList(SamXParser.NameListContext ctx)
   {
      HashSet<String> values = new HashSet<>();

      for (TerminalNode tn : ctx.NAME())
      {
         values.add(tn.getText());
      }

      return values;
   }

   @Override
   public Object visitAlternativeCondition(SamXParser.AlternativeConditionContext ctx)
   {
      Object firstEnabled = visit(ctx.firstCond);
      if (Boolean.TRUE.equals(firstEnabled))
      {
         return Boolean.TRUE;
      }
      else
      {
         return visit(ctx.secondCond);
      }
   }

   @Override
   public Object visitCombinedCondition(SamXParser.CombinedConditionContext ctx)
   {
      Object firstEnabled = visit(ctx.firstCond);
      if (Boolean.FALSE.equals(firstEnabled))
      {
         return Boolean.FALSE;
      }
      else
      {
         return visit(ctx.secondCond);
      }
   }

   @Override
   public Object visitConditionalBlock(SamXParser.ConditionalBlockContext ctx)
   {
      if (isDisabled(ctx.condition()))
      {
         return null;
      }

      for (SamXParser.BlockContext pt : ctx.block())
      {
         visit(pt);
      }

      return null;
   }

   @Override
   public Object visitAmpersand(SamXParser.AmpersandContext ctx)
   {
      append("&amp;");
      return null;
   }

   @Override
   public Object visitLessThan(SamXParser.LessThanContext ctx)
   {
      append("&lt;");
      return null;
   }

   @Override
   public Object visitGreaterThan(SamXParser.GreaterThanContext ctx)
   {
      append("&gt;");
      return null;
   }

   @Override
   public Object visitQuote(SamXParser.QuoteContext ctx)
   {
      append("&apos;");
      return null;
   }

   @Override
   public Object visitLiteral(SamXParser.LiteralContext ctx)
   {
      append(ctx.getText());
      return null;
   }

   @Override
   public Object visitEscapeSeq(SamXParser.EscapeSeqContext ctx)
   {
      final String text = ctx.getText();
      if (text.length() > 1)
      {
         final char escaped = text.charAt(1);
         switch (escaped)
         {
            case '\'':
               append("&apos;");
               break;

            case '>':
               append("&gt;");
               break;

            case '<':
               append("&lt;");
               break;

            case '&':
               append("&amp;");
               break;

            default:
               append(escaped);
               break;
         }
      }
      return null;
   }

   @Override
   public Object visitString(SamXParser.StringContext ctx)
   {
      append("&quot;");

      final String text = ctx.getText();
      append(text.substring(1, text.length() - 1));

      append("&quot;");
      return null;
   }

   @Override
   public Object visitReferenceAttr(SamXParser.ReferenceAttrContext ctx)
   {
      append("refid=\"");

      append(ctx.NAME().getText());

      append('"');
      return null;
   }

   @Override
   public Object visitIdentifierAttr(SamXParser.IdentifierAttrContext ctx)
   {
      append("id=\"");

      append(ctx.NAME().getText());

      append('"');
      return null;
   }

   @Override
   public Object visitCodeBlock(SamXParser.CodeBlockContext ctx)
   {
      visitCodeBlockDef(ctx.codeBlockDef());
      return null;
   }

   @Override
   public Object visitCodeBlockDef(SamXParser.CodeBlockDefContext ctx)
   {
      final int codeBlockIndent = VisitorUtils.getTokenIndent(ctx, tokenStream);

      addIndent();
      append('<');
      append(getCodeBlockTag());
      if (! docBookMode)
      {
         append(" language=\"");
         append(ctx.language.getText());
         append("\"");
      }
      append("><![CDATA[");
      appendNewline();

      for (SamXParser.ExternalCodeContext ecc : ctx.externalCode())
      {
         final int codeLineIndent = VisitorUtils.getTokenIndent(ecc, tokenStream);

         for (int ii = codeBlockIndent; ii < codeLineIndent; ++ ii)
         {
            append(' ');
         }

         append(ecc.EXTCODE().getText());
         appendNewline();
      }

      addIndent();
      append("]]>");
      appendCloseTag(getCodeBlockTag());

      return null;
   }

   private void renderDocBookFigure(SamXParser.InsertImageContext ctx)
   {
      AttributeVisitor attributeVisitor = new AttributeVisitor();
      for (SamXParser.AttributeContext ac : ctx.blockMetadata().metadata().attribute())
      {
         attributeVisitor.visit(ac);
      }

      final String elementId = attributeVisitor.getId();
      append(String.format("<figure xml:id=\"%s\">", elementId));
      appendNewline();
      indentLevel++;

      addIndent();

      renderTitle(ctx);

      addIndent();
      append("<mediaobject>");
      appendNewline();
      indentLevel++;

      addIndent();
      append("<imageobject>");
      appendNewline();
      indentLevel++;

      addIndent();
      append("<imagedata fileref=\"");
      visitText(ctx.text());
      append("\" />");
      appendNewline();

      indentLevel--;
      addIndent();
      append("</imageobject>");
      appendNewline();

      indentLevel--;
      addIndent();
      append("</mediaobject>");
      appendNewline();

      indentLevel--;
      addIndent();
      append("</figure>");
      appendNewline();
   }

   private void renderFigure(SamXParser.InsertImageContext ctx)
   {
      append("<imagedata");
      AttributeVisitor attributeVisitor = new AttributeVisitor();
      if (ditaMode)
      {
         attributeVisitor.setDitaMode();
      }
      for (SamXParser.AttributeContext ac : ctx.blockMetadata().metadata().attribute())
      {
         attributeVisitor.visit(ac);
      }
      append(attributeVisitor.toString());

      append(" fileref=\"");
      visitText(ctx.text());
      append("\"");
      if (ctx.blockMetadata().description != null)
      {
         append("><title>");
         visitFlow(ctx.blockMetadata().description);
         append("</title></imagedata>");
      }
      else
      {
         append(" />");
      }
   }

   @Override
   public Object visitInsertImage(SamXParser.InsertImageContext ctx)
   {
      if (isDisabled(ctx))
      {
         return null;
      }

      addIndent();
      if (docBookMode)
      {
         renderDocBookFigure(ctx);
      }
      else
      {
         renderFigure(ctx);
      }
      appendNewline();

      return null;
   }

   @Override
   public StringBuilder visitLocalInsert(SamXParser.LocalInsertContext ctx)
   {
      visitText(ctx.text());

      return null;
   }

   private void renderElementWithAttributesOpen(String tagType, List<SamXParser.AttributeContext> attributes, List<String> extraClass)
   {
      append('<');
      append(tagType);

      AttributeVisitor attributeVisitor = new AttributeVisitor(extraClass);
      if (docBookMode)
      {
         attributeVisitor.setDocBookMode();
      }
      if (ditaMode)
      {
         attributeVisitor.setDitaMode();
      }
      for (SamXParser.AttributeContext ac : attributes)
      {
         attributeVisitor.visit(ac);
      }
      final String renderedAttributes = attributeVisitor.toString();
      append(renderedAttributes);
   }

   private void renderElementWithAttributes(String tagType, List<SamXParser.AttributeContext> attributes)
   {
      renderElementWithAttributesOpen(tagType, attributes, null);
      append('>');
   }

   private void renderElementWithAttributes(String tagType, List<SamXParser.AttributeContext> attributes, List<String> extraClass)
   {
      renderElementWithAttributesOpen(tagType, attributes, extraClass);
      append('>');
   }

   private void renderTitle(ParserRuleContext prc)
   {
      final SamXParser.BlockMetadataContext blockMetadata = prc.getRuleContext(SamXParser.BlockMetadataContext.class, 0);

      if (blockMetadata != null)
      {
         if (blockMetadata.description != null)
         {
            final String descriptionText = blockMetadata.description.getText();
            if (! descriptionText.isEmpty())
            {
               addIndent();
               append("<title>");
               visitFlow(blockMetadata.description);
               append("</title>");
               appendNewline();
            }
         }
      }
   }

   @Override
   public StringBuilder visitGeneralGrid(SamXParser.GeneralGridContext ctx)
   {
      if (isDisabled(ctx))
      {
         return null;
      }

      /*
       * parse
       */

      final GridVisitor.GeneralGridGroup body = new GridVisitor.GeneralGridGroup(ctx.body, null);

      GridVisitor.GeneralGridGroup header = null;
      if (ctx.header != null)
      {
         header = new GridVisitor.GeneralGridGroup(ctx.header, null);
         if (header.columnCount != body.columnCount)
         {
            throw new RuntimeException(String.format("Invalid table specification: multiple table column sizes between header (%d) and body (%d)", header.columnWidths.length, body.columnWidths.length));
         }
      }

      GridVisitor.GeneralGridGroup footer = null;
      if (ctx.footer != null)
      {
         footer = new GridVisitor.GeneralGridGroup(ctx.footer, null);
         if (footer.columnCount != body.columnCount)
         {
            throw new RuntimeException(String.format("Invalid table specification: multiple table column sizes between footer (%d) and body (%d)", footer.columnWidths.length, body.columnWidths.length));
         }
      }

      /*
       * render
       */

      addIndent();
      renderElementWithAttributes("table", ctx.blockMetadata().metadata().attribute());
      appendNewline();

      indentLevel++;

      renderTitle(ctx);

      if (docBookMode)
      {
         addIndent();
         append(String.format("<tgroup cols=\"%d\">", body.columnCount));
         appendNewline();

         for (int ii = 1; ii <= body.columnCount; ++ ii)
         {
            addIndent();
            append(String.format("<colspec colname=\"c%d\"/>", ii));
            appendNewline();
         }
      }

      if (header != null)
      {
         renderGeneralTableGroup(header, "thead", getTableHeaderDataTag());
      }

      if (footer != null)
      {
         renderGeneralTableGroup(footer, "tfoot", getTableDataTag());
      }

      renderGeneralTableGroup(body, "tbody", getTableDataTag());

      if (docBookMode)
      {
         indentLevel--;
         addIndent();
         append("</tgroup>");
         appendNewline();
      }

      indentLevel--;

      appendCloseTag("table");

      return null;
   }

   private void renderGeneralTableGroup(GridVisitor.GeneralGridGroup gridGroup, String groupName, String dataName)
   {
      if (docBookMode)
      {
         addIndent();
         append('<');
         append(groupName);
         append('>');
         appendNewline();
         indentLevel++;
      }

      int[] rowSpans = new int[gridGroup.columnCount];

      for (GridVisitor.GeneralGridRow ggr : gridGroup.rows)
      {
         if (isDisabled(ggr.metadataContext.condition()))
         {
            continue;
         }

         addIndent();
         renderElementWithAttributes(getTableRowTag(), ggr.metadataContext.attribute());
         appendNewline();
         indentLevel++;

         final ArrayList<GridVisitor.GridCell> rowCells = ggr.cells;
         int jj = 0;
         while (jj < gridGroup.columnCount)
         {
            if (rowSpans[jj] > 0)
            {
               rowSpans[jj]--;
               final int beginEmptySpan = jj;

               jj++;
               while ((jj < gridGroup.columnCount) && (rowSpans[jj] > 0))
               {
                  rowSpans[jj]--;

                  jj++;
               }

               final int endEmptySpan = jj;
               addIndent();
               append(String.format("<!-- span namest=\"c%d\" nameend=\"c%d\" -->", beginEmptySpan, endEmptySpan));
               appendNewline();

               continue;
            }

            final GridVisitor.GridCell gc = rowCells.get(jj);

            addIndent();

            renderElementWithAttributesOpen(dataName, gc.attributes, null);
            if (gc.colSpan > 1)
            {
               if (docBookMode)
               {
                  append(String.format(" namest=\"c%d\" nameend=\"c%d\"", jj + 1, jj + gc.colSpan));
               }
               else
               {
                  append(String.format(" colspan=\"%d\"", gc.colSpan));
               }
            }
            if (gc.rowSpan > 1)
            {
               if (docBookMode)
               {
                  append(String.format(" morerows=\"%d\"", gc.rowSpan - 1));
                  for (int kk = jj; kk < jj + gc.colSpan; ++ kk)
                  {
                     rowSpans[kk] = gc.rowSpan - 1;
                  }
               }
               else
               {
                  append(String.format("<!-- fixme; need rowspan: %d -->", gc.rowSpan));
               }
            }

            if ((gc.flow != null && (! gc.flow.getText().isEmpty())))
            {
               append('>');
               gc.renderContent(this);
               append('<');
               append('/');
               append(dataName);
            }
            else
            {
               append('/');
            }
            append('>');
            appendNewline();

            jj += gc.colSpan;
         }

         indentLevel--;
         appendCloseTag(getTableRowTag());
      }

      if (docBookMode)
      {
         indentLevel--;
         addIndent();
         append('<');
         append('/');
         append(groupName);
         append('>');
         appendNewline();
      }
   }

   public void setDocBookMode()
   {
      docBookMode = true;
   }

   public void setDitaMode()
   {
      ditaMode = true;
   }

   private String getParagraphTag()
   {
      if (docBookMode)
      {
         return "para";
      }
      else
      {
         return "p";
      }
   }

   private String getOrderedListTag()
   {
      if (docBookMode)
      {
         return "orderedlist";
      }
      else
      {
         return "ol";
      }
   }

   private String getUnorderedListTag()
   {
      if (docBookMode)
      {
         return "itemizedlist";
      }
      else
      {
         return "ul";
      }
   }

   private String getListItemTag()
   {
      if (docBookMode)
      {
         return "listitem";
      }
      else
      {
         return "li";
      }
   }

   private String getTableRowTag()
   {
      if (docBookMode)
      {
         return "row";
      }
      else
      {
         return "tr";
      }
   }

   private String getTableDataTag()
   {
      if (docBookMode)
      {
         return "entry";
      }
      else
      {
         return "td";
      }
   }

   private String getTableHeaderDataTag()
   {
      if (docBookMode)
      {
         return "entry";
      }
      else
      {
         return "th";
      }
   }

   private String getCodeBlockTag()
   {
      if (docBookMode)
      {
         return "programlisting";
      }
      else
      {
         return "codeblock";
      }
   }

   @Override
   public Object visitUrl(SamXParser.UrlContext ctx)
   {
      append(ctx.getText());
      return null;
   }
}
