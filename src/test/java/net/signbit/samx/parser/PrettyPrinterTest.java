package net.signbit.samx.parser;

import org.junit.Test;

import net.signbit.samx.Parser;
import net.signbit.samx.PrettyPrinterVisitor;
import static org.junit.Assert.assertEquals;

public class PrettyPrinterTest
{

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
      final String original = TestUtils.getResourceContents(resourceName);

      final String pretty = prettify(original);

      assertEquals(original, pretty);
   }

   private void testAsPretty(String resourceName, String prettifiedResource)
   {
      testIsPretty(prettifiedResource);

      final String original = TestUtils.getResourceContents(resourceName);

      final String pretty = prettify(original);

      final String prettified = TestUtils.getResourceContents(prettifiedResource);

      assertEquals(prettified, pretty);
   }

   @Test
   public void test7()
   {
      testAsPretty("nested_typed_blocks.samx", "nested_typed_blocks-pretty.samx");
   }

   @Test
   public void testTables()
   {
      testAsPretty("record_set.samx", "record_set-pretty.samx");

      testAsPretty("conditions/conditional_rows.samx", "conditions/conditional_rows-pretty.samx");
   }

   @Test
   public void testSpecialText()
   {
      testIsPretty("special_text.samx");
   }

   @Test
   public void testLists()
   {
      testIsPretty("lists/simple_list.samx");

      testIsPretty("lists/lists.samx");

      testIsPretty("lists/nested_lists.samx");

      testAsPretty("lists/multi_line.samx", "lists/multi_line-pretty.samx");
   }

   @Test
   public void testStrings()
   {
      testAsPretty("strings.samx", "strings-pretty.samx");
   }

   @Test
   public void testBlock()
   {
      testAsPretty("single_paragraph.samx", "single_paragraph-pretty.samx");

      testAsPretty("simple_paragraphs.samx", "simple_paragraphs-pretty.samx");

      testAsPretty("typed_block.samx", "typed_block-pretty.samx");

      testAsPretty("ignore_repeated_empty_lines.samx", "ignore_repeated_empty_lines-pretty.samx");
   }

   @Test
   public void testConditionals()
   {
      testAsPretty("conditions/condblock.samx", "conditions/condblock-pretty.samx");

      testAsPretty("conditions/combined.samx", "conditions/combined-pretty.samx");

      testAsPretty("conditions/mixed.samx", "conditions/mixed-pretty.samx");
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

      testIsPretty("annotations.samx");
   }
}
