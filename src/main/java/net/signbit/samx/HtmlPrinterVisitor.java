package net.signbit.samx;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import net.signbit.samx.parser.SamXLexer;
import net.signbit.samx.parser.SamXParser;

public class HtmlPrinterVisitor extends RendererVisitor
{
   private STGroup htmlGroup;

   public HtmlPrinterVisitor(Writer aWriter, HashMap<String, Parser.Result> docDict, HashMap<String, IOException> errDict, HashMap<String, String> referenceDict)
   {
      super(aWriter, docDict, errDict, referenceDict);

      htmlGroup = new STGroupFile("net/signbit/samx/html.stg");
   }

   @Override
   public Object visitDocument(SamXParser.DocumentContext ctx)
   {
      ST document = htmlGroup.getInstanceOf("/document");

      ArrayList<String> renderedBlocks = new ArrayList<>();

      for (SamXParser.BlockContext bc: ctx.block())
      {
         Object rendered = visit(bc);
         if (rendered != null)
         {
            renderedBlocks.add(rendered.toString());
         }
      }

      document.add("title", "converted file");
      document.add("block", renderedBlocks);
      append(document.render());

      return null;
   }

   @Override
   public String visitParagraph(SamXParser.ParagraphContext ctx)
   {
      ST template = htmlGroup.getInstanceOf("/paragraph");

      ArrayList<String> elements = new ArrayList<>();
      for (SamXParser.FlowContext fc : ctx.flow())
      {
         elements.add(visit(fc).toString());
      }

      template.add("text", elements);

      return template.render();
   }

   @Override
   public String visitTypedBlock(SamXParser.TypedBlockContext ctx)
   {
      ST template = htmlGroup.getInstanceOf("/typed_block");

      template.add("type", ctx.NAME().getText());
      if (ctx.description != null)
      {
         template.add("description", visitFlow(ctx.description));
      }

      ArrayList<String> renderedBlocks = new ArrayList<>();
      for (SamXParser.BlockContext bc : ctx.block())
      {
         Object rendered = visit(bc);
         if (rendered != null)
         {
            renderedBlocks.add(rendered.toString());
         }
      }

      template.add("block", renderedBlocks);

      return template.render();
   }

   @Override
   public Object visitFlow(SamXParser.FlowContext ctx)
   {
      StringBuilder builder = new StringBuilder();

      boolean firstToken = true;
      for (ParseTree pt : ctx.children)
      {
         if (firstToken)
         {
            firstToken = false;
         }
         else
         {
            append(' ');
         }

         builder.append(visit(pt));
      }

      return builder.toString();
   }

   @Override
   public String visitText(SamXParser.TextContext ctx)
   {
      StringBuilder builder = new StringBuilder();

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
                  builder.append(' ');
               }
            }
         }

         Object rendered = visit(pt);
         if (rendered != null)
         {
            builder.append(rendered.toString());
            firstToken = false;
         }
      }

      return builder.toString();
   }

   @Override
   public String visitLiteral(SamXParser.LiteralContext ctx)
   {
      return ctx.getText();
   }
}
