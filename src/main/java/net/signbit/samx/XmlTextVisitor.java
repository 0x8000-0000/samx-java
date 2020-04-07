package net.signbit.samx;

import java.io.BufferedWriter;
import java.io.IOException;

import org.antlr.v4.runtime.tree.ParseTree;

import net.signbit.samx.parser.SamXBaseVisitor;
import net.signbit.samx.parser.SamXParser;

public class XmlTextVisitor extends SamXBaseVisitor<Exception>
{
   private BufferedWriter writer;
   private int charactersWritten = 0;

   private Exception exception = null;

   private int indentLevel = 0;

   private boolean indentParagraph = true;
   private boolean writeNewlines = true;

   public XmlTextVisitor(BufferedWriter aWriter)
   {
      writer = aWriter;
   }

   private void addIndent()
   {
      for (int ii = 0; ii < indentLevel; ++ii)
      {
         append("  ");
      }
   }

   private void addIndent(StringBuilder builder)
   {
      for (int ii = 0; ii < indentLevel; ++ii)
      {
         builder.append("  ");
      }
   }

   private void append(StringBuilder aBuilder)
   {
      try
      {
         final String aString = aBuilder.toString();
         writer.write(aString);
         charactersWritten += aString.length();
      }
      catch (IOException ioe)
      {
         exception = ioe;
      }
   }

   private void append(String aString)
   {
      try
      {
         writer.write(aString);
         charactersWritten += aString.length();
      }
      catch (IOException ioe)
      {
         exception = ioe;
      }
   }

   private void append(char aChar)
   {
      try
      {
         writer.write(aChar);
         charactersWritten ++;
      }
      catch (IOException ioe)
      {
         exception = ioe;
      }
   }

   private void appendNewline()
   {
      try
      {
         if (writeNewlines)
         {
            writer.newLine();
         }
      }
      catch (IOException ioe)
      {
         exception = ioe;
      }
   }

   @Override
   public Exception visitDocument(SamXParser.DocumentContext ctx)
   {
      append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
      append("<document>\n");

      for (ParseTree pt: ctx.children)
      {
         visit(pt);
      }

      append("</document>\n");

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
      closeTag.append('\n');
      append(closeTag);

      return null;
   }

   @Override
   public Exception visitFlow(SamXParser.FlowContext ctx)
   {
      int offset = charactersWritten;

      for (ParseTree pt : ctx.children)
      {
         if (offset != charactersWritten)
         {
            append(' ');
            offset = charactersWritten;
         }

         visit(pt);
      }

      return null;
   }

   @Override
   public Exception visitPhrase(SamXParser.PhraseContext ctx)
   {
      append("<phrase>");
      append(ctx.text().getText());
      append("</phrase>");

      return null;
   }

   @Override
   public Exception visitText(SamXParser.TextContext ctx)
   {
      int offset = charactersWritten;

      for (ParseTree pt: ctx.children)
      {
         if (offset != charactersWritten)
         {
            append(' ');
            offset = charactersWritten;
         }

         final String text = pt.getText();
         append(text);
      }

      return null;
   }

   @Override
   public Exception visitEmpty(SamXParser.EmptyContext ctx)
   {
      return null;
   }

   @Override
   public Exception visitTypedBlock(SamXParser.TypedBlockContext ctx)
   {
      final String typeText = ctx.NAME().getText();
      addIndent();
      append('<');
      append(typeText);
      append('>');
      append('\n');

      indentLevel ++;

      final String descriptionText = ctx.description.getText();
      if (! descriptionText.isEmpty())
      {
         addIndent();
         append("<title>");
         visit(ctx.description);
         append("</title>\n");
      }

      for (ParseTree pt: ctx.block())
      {
         visit(pt);
      }

      indentLevel --;

      addIndent();
      append('<');
      append('/');
      append(typeText);
      append('>');
      append('\n');

      return null;
   }

   @Override
   public Exception visitParagraph(SamXParser.ParagraphContext ctx)
   {
      if (indentParagraph)
      {
         addIndent();
      }

      append("<p>");

      int offset = charactersWritten;

      for (SamXParser.FlowContext fc: ctx.flow())
      {
         if (offset != charactersWritten)
         {
            append(' ');
            offset = charactersWritten;
         }

         visit(fc);
      }

      append("</p>");

      if (indentParagraph)
      {
         appendNewline();
      }

      return null;
   }

   @Override
   public Exception visitUnorderedList(SamXParser.UnorderedListContext ctx)
   {
      addIndent();
      appendNewline();
      append("<ul>");
      appendNewline();

      indentLevel++;

      final boolean saveIndentParagraph = indentParagraph;
      indentParagraph = false;

      for (SamXParser.ParagraphContext pc: ctx.paragraph())
      {
         addIndent();

         append("<li>");
         visit(pc);
         append("</li>");

         appendNewline();
      }

      indentParagraph = saveIndentParagraph;

      indentLevel--;

      addIndent();
      append("</ul>");
      appendNewline();

      return null;
   }

   @Override
   public Exception visitRecordSet(SamXParser.RecordSetContext ctx)
   {
      final String typeText = ctx.NAME().getText();

      addIndent();
      append('<');
      append(typeText);
      append('>');
      appendNewline();

      indentLevel++;

      final SamXParser.HeaderRowContext header = ctx.headerRow();

      for (SamXParser.RecordRowContext rrc: ctx.recordRow())
      {
         addIndent();
         append("<record>");
         appendNewline();

         indentLevel ++;

         for (int ii = 0; ii < header.NAME().size(); ii ++)
         {
            addIndent();
            append('<');
            append(header.NAME(ii).getText());
            append('>');

            visitFlow(rrc.flow(ii));

            append('<');
            append('/');
            append(header.NAME(ii).getText());
            append('>');
            appendNewline();
         }

         indentLevel --;

         addIndent();
         append("</record>");
         appendNewline();
      }

      indentLevel--;

      addIndent();
      append('<');
      append('/');
      append(typeText);
      append('>');
      appendNewline();

      return null;
   }

   @Override
   public Exception visitRecordRow(SamXParser.RecordRowContext ctx)
   {
      return null;
   }
}