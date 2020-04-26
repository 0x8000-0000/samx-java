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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import org.antlr.v4.runtime.BufferedTokenStream;

import net.signbit.samx.parser.SamXParser;
import net.signbit.samx.parser.SamXParserBaseVisitor;

public class EmbeddedCodeVisitor extends SamXParserBaseVisitor<StringBuilder>
{
   private final BufferedTokenStream tokenStream;
   private final File parentDir;
   private final HashSet<String> elements;


   public EmbeddedCodeVisitor(BufferedTokenStream tokenStream, File parentDir, String[] elements)
   {
      this.tokenStream = tokenStream;
      this.parentDir = parentDir;
      this.elements = new HashSet<>();
      if (elements != null)
      {
         this.elements.addAll(Arrays.asList(elements));
      }
   }

   @Override
   public StringBuilder visitDocument(SamXParser.DocumentContext ctx)
   {
      for (SamXParser.BlockContext bc : ctx.block())
      {
         visit(bc);
      }

      return null;
   }

   @Override
   public StringBuilder visitTypedBlock(SamXParser.TypedBlockContext ctx)
   {
      for (SamXParser.BlockContext bc : ctx.block())
      {
         visit(bc);
      }

      return null;
   }

   @Override
   public StringBuilder visitCodeBlock(SamXParser.CodeBlockContext ctx)
   {
      AttributeVisitor attributeVisitor = new AttributeVisitor();
      attributeVisitor.visit(ctx.metadata());

      final String fileStem = attributeVisitor.getId();

      final String fileExtension = ctx.language.getText();

      if ((!elements.isEmpty()) && (!elements.contains(fileStem)))
      {
         System.out.println(String.format("Skipping unselected element %s (%s)", fileStem, fileExtension));
         return null;
      }

      final File outputFile = new File(parentDir, fileStem + "." + fileExtension);

      try
      {
         System.out.println("Writing " + outputFile.getCanonicalPath());

         final FileWriter writer = new FileWriter(outputFile);

         /*
          * find the leftmost code line - that will become column 0
          */
         int codeBlockIndent = 65536;
         for (SamXParser.ExternalCodeContext ecc : ctx.externalCode())
         {
            final int codeLineIndent = VisitorUtils.getTokenIndent(ecc, tokenStream);
            if (codeBlockIndent > codeLineIndent)
            {
               codeBlockIndent = codeLineIndent;
            }
         }

         for (SamXParser.ExternalCodeContext ecc : ctx.externalCode())
         {
            final int codeLineIndent = VisitorUtils.getTokenIndent(ecc, tokenStream);

            for (int ii = codeBlockIndent; ii < codeLineIndent; ++ ii)
            {
               writer.append(' ');
            }

            writer.append(ecc.EXTCODE().getText());
            writer.append('\n');
         }

         writer.close();
      }
      catch (IOException ioe)
      {
         System.err.println("Cannot open file " + outputFile.getAbsolutePath() + " for writing: " + ioe.getMessage());
      }

      return null;
   }
}
