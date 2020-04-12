package net.signbit.samx.parser;

import java.io.StringWriter;

import org.junit.Test;

import net.signbit.samx.Parser;
import net.signbit.samx.XmlTextVisitor;

import static org.junit.Assert.assertEquals;

public class XmlConverterTest
{
   private static String convert(String inputString)
   {
      Parser.Result result = Parser.parseString(inputString);

      StringWriter writer = new StringWriter();

      XmlTextVisitor visitor = new XmlTextVisitor(writer, result.includedDocuments, result.includedExceptions, result.referencePaths);
      visitor.setTokenStream(result.tokens);

      visitor.visit(result.document);
      writer.flush();

      return writer.toString();
   }

   private void testConversion(String resourceName, String prettifiedResource)
   {
      final String original = TestUtils.getResourceContents(resourceName);

      final String converted  = convert(original);

      final String expected  = TestUtils.getResourceContents(prettifiedResource);

      assertEquals(expected, converted);
   }

   @Test
   public void testLists()
   {
      testConversion("lists/multi_line.samx", "lists/multi_line.xml");

      testConversion("lists/nested_lists.samx", "lists/nested_lists.xml");
   }
}
