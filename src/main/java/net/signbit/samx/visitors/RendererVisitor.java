/*
   Copyright 2020 Florin Iucha

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package net.signbit.samx.visitors;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;

import net.signbit.samx.Parser;
import net.signbit.samx.parser.SamXParser;
import net.signbit.samx.parser.SamXParserBaseVisitor;

public class RendererVisitor extends SamXParserBaseVisitor<Object>
{
   final BufferedTokenStream tokenStream;
   final PlainTextVisitor plainTextVisitor;

   final Writer writer;
   final HashMap<String, Parser.Result> includedDocuments;
   final HashMap<String, IOException> includedExceptions;
   final HashMap<String, String> referencePaths;

   Properties properties = new Properties();
   protected final Set<String> trueFlags = new HashSet<>();
   protected final Set<String> falseFlags = new HashSet<>();

   int charactersWritten = 0;
   Exception exception = null;

   boolean writeNewlines = true;
   boolean writeIndent = true;

   int indentLevel = 0;


   public RendererVisitor(Writer aWriter, HashMap<String, Parser.Result> docDict, HashMap<String, IOException> errDict, HashMap<String, String> referenceDict, BufferedTokenStream tokenStream)
   {
      writer = aWriter;
      includedDocuments = docDict;
      includedExceptions = errDict;
      referencePaths = referenceDict;
      this.tokenStream = tokenStream;

      plainTextVisitor = new PlainTextVisitor(tokenStream);
   }

   public void setProperties(Properties inputProperties)
   {
      properties = inputProperties;
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

      Boolean result = Boolean.FALSE;

      if (configuredValue != null)
      {
         final String operator = ctx.oper.getText();
         if (configuredValue.equals(value) && (operator.charAt(0) == '='))
         {
            result = Boolean.TRUE;
         }

         if ((! configuredValue.equals(value)) && (operator.charAt(0) == '!'))
         {
            result = Boolean.TRUE;
         }
      }

      //System.err.println(String.format("%s %s %s (%s) -> %s", ctx.variable.getText(), ctx.oper.getText(), ctx.value.getText(), configuredValue, result));

      return result;
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
            if (! potentialValues.contains(configuredValue))
            {
               return Boolean.TRUE;
            }
         }
      }

      return Boolean.FALSE;
   }

   public boolean isDisabled(SamXParser.ConditionContext cc)
   {
      if (cc != null)
      {
         Object enabled = visit(cc);
         return ! Boolean.TRUE.equals(enabled);
      }
      else
      {
         return false;
      }
   }

   public boolean isDisabled(ParserRuleContext prc)
   {
      final SamXParser.MetadataContext metadata = prc.getRuleContext(SamXParser.MetadataContext.class, 0);

      if ((metadata != null) && (metadata.condition() != null))
      {
         Object enabled = visit(metadata.condition());
         return ! Boolean.TRUE.equals(enabled);
      }
      else
      {
         return false;
      }
   }

   void append(StringBuilder aBuilder)
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

   public void append(String aString)
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

   void append(char aChar)
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

   void appendNewline()
   {
      try
      {
         if (writeNewlines)
         {
            writer.append('\n');
         }
      }
      catch (IOException ioe)
      {
         exception = ioe;
      }
   }

   public void skipNewLines()
   {
      writeNewlines = false;
   }

   public void skipIndent()
   {
      writeIndent = false;
   }

   void addIndent()
   {
      if (writeIndent)
      {
         for (int ii = 0; ii < indentLevel; ++ ii)
         {
            append("  ");
         }
      }
   }

   void addIndent(StringBuilder builder)
   {
      for (int ii = 0; ii < indentLevel; ++ ii)
      {
         builder.append("  ");
      }
   }

   /*
    * fragment support
    */
   private final HashMap<String, SamXParser.DefineFragmentContext> fragments = new HashMap<>();

   @Override
   public Object visitDefineFragment(SamXParser.DefineFragmentContext ctx)
   {
      fragments.put(ctx.name.getText(), ctx);
      return null;
   }

   @Override
   public Object visitInsertFragment(SamXParser.InsertFragmentContext ctx)
   {
      if (isDisabled(ctx))
      {
         return null;
      }

      SamXParser.DefineFragmentContext defineCtx = fragments.get(ctx.name.getText());

      for (SamXParser.BlockContext bc : defineCtx.block())
      {
         visit(bc);
      }

      return null;
   }

   public String getPlainText(SamXParser.FlowContext fc)
   {
      if (fc == null)
      {
         return "";
      }
      else
      {
         return plainTextVisitor.visitFlow(fc).toString();
      }
   }

   public static AttributeVisitor getAttributes(ParserRuleContext prc)
   {
      AttributeVisitor attributeVisitor = new AttributeVisitor();
      for (SamXParser.AttributeContext ac : prc.getRuleContexts(SamXParser.AttributeContext.class))
      {
         attributeVisitor.visit(ac);
      }
      return attributeVisitor;
   }
}
