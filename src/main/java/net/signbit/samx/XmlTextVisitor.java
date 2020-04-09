package net.signbit.samx;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import net.signbit.samx.parser.SamXLexer;
import net.signbit.samx.parser.SamXParserBaseVisitor;
import net.signbit.samx.parser.SamXParser;

public class XmlTextVisitor extends SamXParserBaseVisitor<Object>
{
   private final BufferedWriter writer;
   private int charactersWritten = 0;

   private BufferedTokenStream tokenStream;

   private Exception exception = null;

   private int indentLevel = 0;

   private boolean indentParagraph = true;
   private boolean writeXmlDeclaration = true;
   private boolean writeNewlines = true;

   private final HashMap<String, Parser.Result> includedDocuments;
   private final HashMap<String, IOException> includedExceptions;
   private final HashMap<String, String> referencePaths;
   private Properties properties = new Properties();
   private final Set<String> trueFlags = new HashSet<>();
   private final Set<String> falseFlags = new HashSet<>();

   private String topElement = "document";
   private String topElementNamespace = null;
   private String topElementVersion = null;

   public XmlTextVisitor(BufferedWriter aWriter, HashMap<String, Parser.Result> docDict, HashMap<String, IOException> errDict, HashMap<String, String> referenceDict)
   {
      writer = aWriter;
      includedDocuments = docDict;
      includedExceptions = errDict;
      referencePaths = referenceDict;
   }

   public void skipXmlDeclaration()
   {
      writeXmlDeclaration = false;
   }

   public void setIndentLevel(int newLevel)
   {
      indentLevel = newLevel;
   }

   public void setTopElement(String name)
   {
      topElement = name;
   }

   public void setTopElementNamespace(String name)
   {
      topElementNamespace = name;
   }

   public void setTopElementVersion(String name)
   {
      topElementVersion = name;
   }

   public void skipNewLines()
   {
      writeNewlines = false;
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
         charactersWritten++;
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
      if (writeXmlDeclaration)
      {
         append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
         append('<');
         append(topElement);

         if (topElementNamespace != null)
         {
            append(" xmlns=\"");
            append(topElementNamespace);
            append('"');

            if (topElementVersion != null)
            {
               append(" version=\"");
               append(topElementVersion);
               append('"');
            }
         }

         append('>');
         appendNewline();
      }

      for (SamXParser.BlockContext bc : ctx.block())
      {
         visit(bc);
      }

      if (writeXmlDeclaration)
      {
         append("</");
         append(topElement);
         append('>');
         appendNewline();
      }

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
   public Object visitFlow(SamXParser.FlowContext ctx)
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
   public Object visitPhrase(SamXParser.PhraseContext ctx)
   {
      if (! isEnabled(ctx.condition()))
      {
         return null;
      }

      append("<phrase>");
      visit(ctx.text());
      append("</phrase>");

      return null;
   }

   @Override
   public Exception visitText(SamXParser.TextContext ctx)
   {
      int offset = charactersWritten;

      boolean firstToken = true;

      for (ParseTree pt : ctx.children)
      {
         if (offset != charactersWritten)
         {
            if (firstToken)
            {
               firstToken = true;
            }
            else
            {
               final Interval pos = pt.getSourceInterval();
               if (pos.a == pos.b)
               {
                  final List<Token> precedingTokens = tokenStream.getHiddenTokensToLeft(pos.a, SamXLexer.WHITESPACE);
                  if ((precedingTokens != null) && (!precedingTokens.isEmpty()))
                  {
                     append(' ');
                  }
               }
            }

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
   public Object visitTypedBlock(SamXParser.TypedBlockContext ctx)
   {
      if (! isEnabled(ctx.condition()))
      {
         return null;
      }

      final String typeText = ctx.NAME().getText();
      addIndent();
      append('<');
      append(typeText);
      append('>');
      append('\n');

      indentLevel++;

      if (ctx.description != null)
      {
         final String descriptionText = ctx.description.getText();
         if (!descriptionText.isEmpty())
         {
            addIndent();
            append("<title>");
            visit(ctx.description);
            append("</title>\n");
         }
      }

      for (ParseTree pt : ctx.block())
      {
         visit(pt);
      }

      indentLevel--;

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

      append("<para>");

      int offset = charactersWritten;

      for (SamXParser.FlowContext fc : ctx.flow())
      {
         if (offset != charactersWritten)
         {
            append(' ');
            offset = charactersWritten;
         }

         visit(fc);
      }

      append("</para>");

      if (indentParagraph)
      {
         appendNewline();
      }

      return null;
   }

   private void visitGenericList(String tagType, List<SamXParser.ListElementContext> elements)
   {
      addIndent();
      appendNewline();
      append('<');
      append(tagType);
      append('>');
      appendNewline();

      indentLevel++;

      final boolean saveIndentParagraph = indentParagraph;
      indentParagraph = false;

      for (SamXParser.ListElementContext lec : elements)
      {
         Object enabled = visit(lec.condition());
         if (Boolean.TRUE.equals(enabled))
         {
            addIndent();

            append("<li>");
            for (SamXParser.ParagraphContext pc : lec.paragraph())
            {
               visit(pc);
            }
            append("</li>");

            appendNewline();
         }
      }

      indentParagraph = saveIndentParagraph;

      indentLevel--;

      addIndent();
      append("</");
      append(tagType);
      append('>');
      appendNewline();
   }

   @Override
   public Exception visitUnorderedList(SamXParser.UnorderedListContext ctx)
   {
      visitGenericList("ul", ctx.listElement());

      return null;
   }

   @Override
   public Object visitOrderedList(SamXParser.OrderedListContext ctx)
   {
      visitGenericList("ol", ctx.listElement());

      return null;
   }

   private boolean isEnabled(SamXParser.ConditionContext condition)
   {
      if (condition != null)
      {
         Object enabled = visit(condition);
         return Boolean.TRUE.equals(enabled);
      }
      else
      {
         return true;
      }
   }

   @Override
   public Object visitRecordSet(SamXParser.RecordSetContext ctx)
   {
      if (! isEnabled(ctx.condition()))
      {
         return null;
      }

      final String typeText = ctx.NAME().getText();

      addIndent();
      append('<');
      append(typeText);
      append('>');
      appendNewline();

      indentLevel++;

      final SamXParser.HeaderRowContext header = ctx.headerRow();

      for (SamXParser.RecordRowContext rrc : ctx.recordRow())
      {
         if (! isEnabled(rrc.condition()))
         {
            continue;
         }

         addIndent();
         append("<record>");
         appendNewline();

         indentLevel++;

         for (int ii = 0; ii < header.NAME().size(); ii++)
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

         indentLevel--;

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
   public Exception visitIncludeFile(SamXParser.IncludeFileContext ctx)
   {
      if (! isEnabled(ctx.condition()))
      {
         return null;
      }

      StringBuilder builder = new StringBuilder();

      builder.append("<!-- ");
      builder.append("include: ");

      final String reference = ctx.reference.getText();

      builder.append(reference);

      String absolutePath = referencePaths.get(reference);

      Parser.Result includedResult = includedDocuments.get(absolutePath);
      if (includedResult == null)
      {
         builder.append(" is not found: ");

         IOException ioe = includedExceptions.get(absolutePath);

         if (ioe != null)
         {
            builder.append(ioe.getMessage());
         }
         else
         {
            builder.append(" exception missing");
         }
      }

      builder.append(" -->");

      append(builder);
      appendNewline();

      if (includedResult != null)
      {
         XmlTextVisitor visitor = new XmlTextVisitor(writer, includedDocuments, includedExceptions, includedResult.referencePaths);
         visitor.skipXmlDeclaration();
         visitor.setIndentLevel(indentLevel + 1);
         visitor.setTokenStream(includedResult.tokens);

         visitor.visit(includedResult.document);

         StringBuilder endBuilder = new StringBuilder();
         endBuilder.append("<!-- ");
         endBuilder.append("include: ");
         endBuilder.append(reference);
         endBuilder.append(" -->");
         append(endBuilder);
         appendNewline();
      }

      return null;
   }

   public void setProperties(Properties inputProperties)
   {
      properties = inputProperties;
   }

   @Override
   public Object visitCondition(SamXParser.ConditionContext ctx)
   {
      return visit(ctx.conditionExpr());
   }

   @Override
   public Object visitBooleanTrueCondition(SamXParser.BooleanTrueConditionContext ctx)
   {
      final String variable = ctx.variable.getText();

      if (trueFlags.contains(variable))
      {
         return Boolean.TRUE;
      }

      final Object val = properties.get(variable);
      if ("true".equals(val))
      {
         return Boolean.TRUE;
      }

      return Boolean.FALSE;
   }

   @Override
   public Object visitBooleanFalseCondition(SamXParser.BooleanFalseConditionContext ctx)
   {
      final String variable = ctx.variable.getText();

      if (falseFlags.contains(variable))
      {
         return Boolean.TRUE;
      }

      final Object val = properties.get(variable);
      if ("false".equals(val))
      {
         return Boolean.TRUE;
      }

      return Boolean.FALSE;
   }

   @Override
   public Object visitComparisonCondition(SamXParser.ComparisonConditionContext ctx)
   {
      final String variable = ctx.variable.getText();
      final String value = ctx.value.getText();

      final String configuredValue = (String) properties.get(variable);

      if (configuredValue != null)
      {
         if (configuredValue.equals(value))
         {
            return Boolean.TRUE;
         }
      }

      return Boolean.FALSE;
   }

   public void setTrueFlags(String[] trueFlagInput)
   {
      if (trueFlagInput != null)
      {
         trueFlags.addAll(Arrays.asList(trueFlagInput));
      }
   }

   public void setFalseFlags(String[] falseFlagInput)
   {
      if (falseFlagInput != null)
      {
         falseFlags.addAll(Arrays.asList(falseFlagInput));
      }
   }

   @SuppressWarnings("unchecked")
   @Override
   public Object visitBelongsToSetCondition(SamXParser.BelongsToSetConditionContext ctx)
   {
      final String variable = ctx.variable.getText();
      final String configuredValue = (String) properties.get(variable);

      if (configuredValue != null)
      {
         HashSet<String> potentialValues = (HashSet<String>) visit(ctx.nameList());
         if (potentialValues != null)
         {
            if (potentialValues.contains(configuredValue))
            {
               return Boolean.TRUE;
            }
         }
      }

      return Boolean.FALSE;
   }

   @SuppressWarnings("unchecked")
   @Override
   public Object visitNotBelongsToSetCondition(SamXParser.NotBelongsToSetConditionContext ctx)
   {
      final String variable = ctx.variable.getText();
      final String configuredValue = (String) properties.get(variable);

      if (configuredValue != null)
      {
         HashSet<String> potentialValues = (HashSet<String>) visit(ctx.nameList());
         if (potentialValues != null)
         {
            if (!potentialValues.contains(configuredValue))
            {
               return Boolean.TRUE;
            }
         }
      }

      return Boolean.FALSE;
   }

   @Override
   public HashSet<String> visitNameList(SamXParser.NameListContext ctx)
   {
      HashSet<String> values = new HashSet<>();

      for (TerminalNode tn : ctx.NAME())
      {
         values.add(tn.getText());
      }

      return values;
   }

   @Override
   public Object visitAlternativeCondition(SamXParser.AlternativeConditionContext ctx)
   {
      Object firstEnabled = visit(ctx.firstCond);
      if (Boolean.TRUE.equals(firstEnabled))
      {
         return Boolean.TRUE;
      }
      else
      {
         return visit(ctx.secondCond);
      }
   }

   @Override
   public Object visitCombinedCondition(SamXParser.CombinedConditionContext ctx)
   {
      Object firstEnabled = visit(ctx.firstCond);
      if (Boolean.FALSE.equals(firstEnabled))
      {
         return Boolean.FALSE;
      }
      else
      {
         return visit(ctx.secondCond);
      }
   }

   public void setTokenStream(BufferedTokenStream tokens)
   {
      tokenStream = tokens;
   }
}
