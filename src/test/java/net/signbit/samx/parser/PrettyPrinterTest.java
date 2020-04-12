package net.signbit.samx.parser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.junit.Test;

import net.signbit.samx.Parser;
import net.signbit.samx.PrettyPrinterVisitor;

import static org.junit.Assert.assertEquals;

public class PrettyPrinterTest
{
   private static String getResourceContents(String resourceName)
   {
      ClassLoader classLoader = ClassLoader.getSystemClassLoader();

      InputStream is = classLoader.getResourceAsStream(resourceName);

      if (is == null)
      {
         throw new RuntimeException("Resource not found");
      }

      InputStreamReader isr = new InputStreamReader(is);
      BufferedReader reader = new BufferedReader(isr);

      return reader.lines().collect(Collectors.joining(System.lineSeparator())) + "\n";
   }

   private static String prettify(String inputString)
   {
      Parser.Result result = Parser.parseString(inputString);
      PrettyPrinterVisitor printer = new PrettyPrinterVisitor();

      printer.setTokenStream(result.tokens);

      StringBuilder builder = printer.visit(result.document);

      return builder.toString();
   }

   private void testIsPretty(String resourceName)
   {
      final String original = getResourceContents(resourceName);

      final String pretty = prettify(original);

      assertEquals(original, pretty);
   }

   private void testAsPretty(String resourceName, String prettifiedResource)
   {
      testIsPretty(prettifiedResource);

      final String original = getResourceContents(resourceName);

      final String pretty = prettify(original);

      final String prettified = getResourceContents(prettifiedResource);

      assertEquals(prettified, pretty);
   }

   @Test
   public void test7()
   {
      testAsPretty("7-2.samx", "7-pretty.samx");
   }

   @Test
   public void testTables()
   {
      testAsPretty("9-1.samx", "9-1-pretty.samx");

      testAsPretty("conditions/2.samx", "conditions/2-pretty.samx");
   }

   @Test
   public void testSpecialText()
   {
      testIsPretty("special_text.samx");
   }

   @Test
   public void testLists()
   {
      testIsPretty("lists.samx");

      testIsPretty("nested-lists.samx");
   }

   @Test
   public void testStrings()
   {
      testAsPretty("strings.samx", "strings-pretty.samx");
   }

   @Test
   public void testBlock()
   {
      testAsPretty("2-1.samx", "2-1-pretty.samx");

      testAsPretty("5-1.samx", "5-1-pretty.samx");
   }

   @Test
   public void testConditionals()
   {
      testAsPretty("conditions/condblock.samx", "conditions/condblock-pretty.samx");

      testAsPretty("conditions/combined.samx", "conditions/combined-pretty.samx");
   }

   @Test
   public void testCodeBlocks()
   {
      testIsPretty("sam/embedded_code.sam");
   }

   @Test
   public void testFields()
   {
      testIsPretty("sam/fields.sam");
   }

   @Test
   public void textText()
   {
      testAsPretty("wrap/long_text.samx", "wrap/long_text-pretty.samx");

      testAsPretty("wrap/long_phrase.samx", "wrap/long_phrase-pretty.samx");

      testAsPretty("wrap/long_lists.samx", "wrap/long_lists-pretty.samx");
   }
}
