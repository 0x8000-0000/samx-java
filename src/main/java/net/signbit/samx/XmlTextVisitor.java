package net.signbit.samx;

import java.io.BufferedWriter;
import java.io.IOException;

import org.antlr.v4.runtime.tree.ParseTree;

import net.signbit.samx.parser.SamXBaseVisitor;
import net.signbit.samx.parser.SamXParser;

public class XmlTextVisitor extends SamXBaseVisitor<Exception>
{
   private BufferedWriter writer;

   private Exception exception = null;

   private int indentLevel = 0;

   public XmlTextVisitor(BufferedWriter aWriter)
   {
      writer = aWriter;
   }

   private void addIndent()
   {
      for (int ii = 0; ii < indentLevel; ++ii)
      {
         append("   ");
      }
   }

   private void addIndent(StringBuilder builder)
   {
      for (int ii = 0; ii < indentLevel; ++ii)
      {
         builder.append("   ");
      }
   }

   private void append(StringBuilder aBuilder)
   {
      try
      {
         writer.write(aBuilder.toString());
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
      int length = 0;

      for (ParseTree pt : ctx.children)
      {
         if (length > 0)
         {
            append(' ');
         }

         visit(pt);
         length += pt.getText().length();
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
      int textLength = 0;

      for (ParseTree pt: ctx.children)
      {
         if (textLength > 0)
         {
            append(' ');
         }

         final String text = pt.getText();
         textLength += text.length();

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
      addIndent();
      append("<p>");

      int textLength = 0;

      for (SamXParser.FlowContext fc: ctx.flow())
      {
         if (textLength > 0)
         {
            append(' ');
         }

         visit(fc);
         textLength += fc.getText().length();
      }

      append("</p>\n");

      return null;
   }
}
