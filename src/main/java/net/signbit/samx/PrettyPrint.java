package net.signbit.samx;

import java.io.IOException;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import net.signbit.samx.parser.SamXLexer;
import net.signbit.samx.parser.SamXParser;

public final class PrettyPrint
{
   public static void main(String[] args) throws IOException
   {
      CharStream input = CharStreams.fromFileName(args[0]);

      SamXLexer lexer = new SamXLexer(input);

      CommonTokenStream tokens = new CommonTokenStream(lexer);

      SamXParser parser = new SamXParser(tokens);

      SamXParser.DocumentContext document = parser.document();

      PrettyPrinterVisitor printer = new PrettyPrinterVisitor();

      StringBuilder builder = printer.visit(document);

      System.out.println(builder.toString());
   }
}
