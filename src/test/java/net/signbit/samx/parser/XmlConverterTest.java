package net.signbit.samx.parser;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

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

      XmlTextVisitor visitor = new XmlTextVisitor(writer, result.includedDocuments, result.includedExceptions, result.referencePaths, result.tokens);

      visitor.visit(result.document);
      writer.flush();

      return writer.toString();
   }

   private static String convert(File inputFile, String root, String namespace, String version) throws IOException
   {
      HashMap<String, Parser.Result> includedDocuments = new HashMap<>();
      HashMap<String, IOException> includedExceptions = new HashMap<>();
      Parser.Result result = Parser.parse(inputFile, includedDocuments, includedExceptions);

      StringWriter writer = new StringWriter();

      XmlTextVisitor visitor = new XmlTextVisitor(writer, result.includedDocuments, result.includedExceptions, result.referencePaths, result.tokens);
      visitor.setTopElement(root);
      visitor.setTopElementNamespace(namespace);
      visitor.setTopElementVersion(version);

      visitor.visit(result.document);
      writer.flush();

      return writer.toString();
   }

   private void testConversion(String resourceName, String prettifiedResource)
   {
      final String original = TestUtils.getResourceContents(resourceName);

      final String converted = convert(original);

      final String expected = TestUtils.getResourceContents(prettifiedResource);

      assertEquals(expected, converted);
   }

   @Test
   public void testLists()
   {
      testConversion("lists/multi_line.samx", "lists/multi_line.xml");

      testConversion("lists/nested_lists.samx", "lists/nested_lists.xml");
   }

   @Test
   public void testFragments()
   {
      testConversion("fragments/simple.samx", "fragments/simple.xml");
   }

   @Test
   public void testDocBook() throws IOException
   {
      final File inputFile = new File("build/resources/test/docbook/main.samx");

      final String converted = convert(inputFile, "book", "http://docbook.org/ns/docbook", "5.1");

      final String expected = TestUtils.getResourceContents("docbook/book.xml");

      assertEquals(expected, converted);
   }

   @Test
   public void testInserts()
   {
      testConversion("insert/simple.samx", "insert/simple.xml");
   }
}
