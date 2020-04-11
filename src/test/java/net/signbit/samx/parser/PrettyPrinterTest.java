package net.signbit.samx.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.stream.Collectors;

import org.junit.Test;

import net.signbit.samx.Parser;
import net.signbit.samx.PrettyPrinterVisitor;

import static org.junit.Assert.assertEquals;

public class PrettyPrinterTest
{
   private void testParse(String resourceName) throws IOException, URISyntaxException
   {
      ClassLoader classLoader = ClassLoader.getSystemClassLoader();

      InputStream is = classLoader.getResourceAsStream(resourceName);

      if (is == null)
      {
         throw new RuntimeException("Resource not found");
      }

      InputStreamReader isr = new InputStreamReader(is);
      BufferedReader reader = new BufferedReader(isr);

      final String original = reader.lines().collect(Collectors.joining(System.lineSeparator())) + "\n";

      Parser.Result result = Parser.parseString(original);
      PrettyPrinterVisitor printer = new PrettyPrinterVisitor();

      printer.setTokenStream(result.tokens);

      StringBuilder builder = printer.visit(result.document);

      final String pretty = builder.toString();

      assertEquals(original, pretty);
   }

   @Test
   public void test7() throws IOException, URISyntaxException
   {
      testParse("7-pretty.samx");
   }

   @Test
   public void testTables() throws IOException, URISyntaxException
   {
      testParse("9-1.samx");
   }

   @Test
   public void testSpecialText() throws IOException, URISyntaxException
   {
      testParse("special_text.samx");
   }

   @Test
   public void testLists() throws IOException, URISyntaxException
   {
      testParse("lists.samx");
   }
}
