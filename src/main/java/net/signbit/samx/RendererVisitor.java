package net.signbit.samx;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CommonTokenStream;

import net.signbit.samx.parser.SamXParser;
import net.signbit.samx.parser.SamXParserBaseVisitor;

public class RendererVisitor extends SamXParserBaseVisitor<Object>
{
   BufferedTokenStream tokenStream;

   final Writer writer;
   final HashMap<String, Parser.Result> includedDocuments;
   final HashMap<String, IOException> includedExceptions;
   final HashMap<String, String> referencePaths;

   Properties properties = new Properties();
   final Set<String> trueFlags = new HashSet<>();
   final Set<String> falseFlags = new HashSet<>();

   int charactersWritten = 0;
   Exception exception = null;

   boolean writeNewlines = true;
   boolean writeIndent = true;

   int indentLevel = 0;


   public RendererVisitor(Writer aWriter, HashMap<String, Parser.Result> docDict, HashMap<String, IOException> errDict, HashMap<String, String> referenceDict)
   {
      writer = aWriter;
      includedDocuments = docDict;
      includedExceptions = errDict;
      referencePaths = referenceDict;
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

   public void setTokenStream(BufferedTokenStream tokens)
   {
      tokenStream = tokens;
   }

   boolean isDisabled(SamXParser.ConditionContext condition)
   {
      if (condition != null)
      {
         Object enabled = visit(condition);
         return !Boolean.TRUE.equals(enabled);
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

   void append(String aString)
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
         for (int ii = 0; ii < indentLevel; ++ii)
         {
            append("  ");
         }
      }
   }

   void addIndent(StringBuilder builder)
   {
      for (int ii = 0; ii < indentLevel; ++ii)
      {
         builder.append("  ");
      }
   }
}
