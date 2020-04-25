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
      visitor.setDocBookMode();

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

      testConversion("wrap/long_lists.samx", "wrap/long_lists.xml");
   }

   @Test
   public void testFragments()
   {
      testConversion("fragments/simple.samx", "fragments/simple.xml");
   }

   public void testDocBook(String inputSource, String docType, String expectedResult) throws IOException
   {
      final File inputFile = new File(inputSource);

      final String converted = convert(inputFile, docType, "http://docbook.org/ns/docbook", "5.1");

      final String expected = TestUtils.getResourceContents(expectedResult);

      assertEquals(expected, converted);
   }

   @Test
   public void testDocBookBook() throws IOException
   {
      testDocBook("build/resources/test/docbook/main.samx", "book", "docbook/book.xml");
   }

   @Test
   public void testInserts()
   {
      testConversion("insert/simple.samx", "insert/simple.xml");
   }

   @Test
   public void testGrids() throws IOException
   {
      testConversion("grids/simple.samx", "grids/simple.xml");

      testConversion("grids/missing_cells.samx", "grids/missing_cells.xml");

      testDocBook("build/resources/test/grids/simple.samx", "article", "grids/simple-docbook.xml");
   }

   @Test
   public void testGeneralGrids() throws IOException
   {
      testConversion("grids/gengrid_rowspan_only.samx", "grids/gengrid_rowspan_only.xml");

      testDocBook("build/resources/test/grids/multispan.samx", "article", "grids/multispan.xml");
   }
}
