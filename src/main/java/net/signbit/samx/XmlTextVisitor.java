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
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

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
         if ((precedingTokens != null) && (!precedingTokens.isEmpty()))
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
         if (!firstElement)
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
      if (isDisabled(ctx.condition()))
      {
         return null;
      }

      renderElementWithAttributes("phrase", ctx.attribute());
      visit(ctx.text());
      append("</phrase>");

      return null;
   }

   @Override
   public Exception visitText(SamXParser.TextContext ctx)
   {
      boolean firstToken = true;

      for (ParseTree pt : ctx.children)
      {
         if (!firstToken)
         {
            final Interval pos = pt.getSourceInterval();
            if (pos.a <= pos.b)
            {
               final List<Token> precedingWhitespace = tokenStream.getHiddenTokensToLeft(pos.a, SamXLexer.WHITESPACE);
               if ((precedingWhitespace != null) && (!precedingWhitespace.isEmpty()))
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
      if (isDisabled(ctx.condition()))
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

      if (ctx.description != null)
      {
         final String descriptionText = ctx.description.getText();
         if (!descriptionText.isEmpty())
         {
            addIndent();
            append("<title>");
            visit(ctx.description);
            append("</title>");
            appendNewline();
         }
      }

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

      append("<para>");

      visitParagraphContents(ctx);

      append("</para>");

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
         if ((lec.condition() == null) || Boolean.TRUE.equals(visit(lec.condition())))
         {
            addIndent();

            append("<li><para>");
            visit(lec.flow());
            if (lec.skipped == null)
            {
               /* This means there is no empty line between the first and second lines in the
                * bulleted list; by the rules of paragraph handling, we have to join them.
                */
               if (!lec.paragraph().isEmpty())
               {
                  append(' ');
                  visitParagraphContents(lec.paragraph(0));
               }
            }
            append("</para>");

            boolean mergedFirstLine = false;
            if (lec.skipped == null)
            {
               mergedFirstLine = true;
            }
            for (SamXParser.ParagraphContext pc : lec.paragraph())
            {
               if (mergedFirstLine)
               {
                  mergedFirstLine = false;
                  continue;
               }
               visit(pc);
            }

            if (lec.unorderedList() != null)
            {
               indentLevel++;
               appendNewline();
               visitGenericList("ul", lec.unorderedList().listElement());
               indentLevel--;
               addIndent();
            }

            if (lec.orderedList() != null)
            {
               indentLevel++;
               appendNewline();
               visitGenericList("ol", lec.orderedList().listElement());
               indentLevel--;
               addIndent();
            }

            append("</li>");

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
      visitGenericList("ul", ctx.listElement());

      return null;
   }

   @Override
   public Object visitOrderedList(SamXParser.OrderedListContext ctx)
   {
      visitGenericList("ol", ctx.listElement());

      return null;
   }

   @Override
   public Object visitRecordSet(SamXParser.RecordSetContext ctx)
   {
      if (isDisabled(ctx.condition()))
      {
         return null;
      }

      final String typeText = ctx.NAME().getText();

      addIndent();
      append('<');
      append(typeText);
      if (ctx.attribute().size() > 0)
      {
         append(' ');
         for (SamXParser.AttributeContext ac : ctx.attribute())
         {
            visit(ac);
         }
      }
      append('>');
      appendNewline();

      indentLevel++;

      final SamXParser.HeaderRowContext header = ctx.headerRow();

      for (SamXParser.RecordRowContext rrc : ctx.recordRow())
      {
         if (isDisabled(rrc.condition()))
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

            if (rrc.optionalFlow(ii).flow() != null)
            {
               append('>');
               visitFlow(rrc.optionalFlow(ii).flow());
               append('<');
               append('/');
               append(header.NAME(ii).getText());
            }
            else
            {
               append('/');
            }

            append('>');
            appendNewline();
         }

         indentLevel--;

         addIndent();
         append("</record>");
         appendNewline();
      }

      indentLevel--;

      appendCloseTag(typeText);

      return null;
   }

   @Override
   public Exception visitIncludeFile(SamXParser.IncludeFileContext ctx)
   {
      if (isDisabled(ctx.condition()))
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
      final int codeBlockIndent = VisitorUtils.getTokenIndent(ctx, tokenStream);

      addIndent();
      append("<codeblock language=\"");
      append(ctx.language.getText());
      append("\"><![CDATA[");
      appendNewline();

      for (SamXParser.ExternalCodeContext ecc : ctx.externalCode())
      {
         final int codeLineIndent = VisitorUtils.getTokenIndent(ecc, tokenStream);

         for (int ii = codeBlockIndent; ii < codeLineIndent; ++ii)
         {
            append(' ');
         }

         append(ecc.EXTCODE().getText());
         appendNewline();
      }

      addIndent();
      append("]]></codeblock>");
      appendNewline();

      return null;
   }

   @Override
   public Object visitInsertImage(SamXParser.InsertImageContext ctx)
   {
      if (isDisabled(ctx.condition()))
      {
         return null;
      }

      addIndent();
      append("<imagedata");
      AttributeVisitor attributeVisitor = new AttributeVisitor();
      for (SamXParser.AttributeContext ac : ctx.attribute())
      {
         attributeVisitor.visit(ac);
      }
      append(attributeVisitor.toString());

      append(" fileref=\"");
      visitText(ctx.text());
      append("\"");
      if (ctx.description != null)
      {
         append(">");
         append("<title>");
         visitFlow(ctx.description);
         append("</title>");
         append("</imagedata>");
      }
      else
      {
         append(" />");
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

   private void renderGridElementDirect(String tagType, SamXParser.GridElementContext gec)
   {
      final SamXParser.FlowContext fc = gec.optionalFlow().flow();

      append('<');
      append(tagType);

      if (fc != null)
      {
         append('>');
         visitFlow(fc);
         append('<');
         append('/');
         append(tagType);
      }
      else
      {
         append('/');
      }
      append('>');
   }

   private void renderElementWithAttributes(String tagType, List<SamXParser.AttributeContext> attributes)
   {
      append('<');
      append(tagType);

      AttributeVisitor attributeVisitor = new AttributeVisitor();
      for (SamXParser.AttributeContext ac : attributes)
      {
         attributeVisitor.visit(ac);
      }
      append(attributeVisitor.toString());

      append('>');
   }

   @Override
   public Object visitGrid(SamXParser.GridContext ctx)
   {
      if (isDisabled(ctx.condition()))
      {
         return null;
      }

      addIndent();
      renderElementWithAttributes("table", ctx.attribute());
      appendNewline();

      indentLevel++;

      addIndent();
      renderElementWithAttributes("tr", ctx.gridHeaderRow().attribute());
      appendNewline();

      final SamXParser.GridHeaderRowContext header = ctx.gridHeaderRow();
      for (SamXParser.GridElementContext gec : header.gridElement())
      {
         addIndent();
         renderGridElementDirect("th", gec);
         appendNewline();
      }

      addIndent();
      append("</tr>");
      appendNewline();

      for (SamXParser.GridRecordRowContext rrc : ctx.gridRecordRow())
      {
         if (isDisabled(rrc.condition()))
         {
            continue;
         }

         addIndent();
         renderElementWithAttributes("tr", rrc.attribute());
         appendNewline();

         indentLevel++;

         for (SamXParser.GridElementContext gec : rrc.gridElement())
         {
            addIndent();
            renderGridElementDirect("td", gec);
            appendNewline();
         }

         indentLevel--;

         addIndent();
         append("</tr>");
         appendNewline();
      }

      indentLevel--;

      appendCloseTag("table");

      return null;
   }
}
