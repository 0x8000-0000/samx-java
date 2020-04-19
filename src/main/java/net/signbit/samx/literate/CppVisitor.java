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

package net.signbit.samx.literate;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;

import org.antlr.v4.runtime.BufferedTokenStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.text.StringTokenizer;
import org.apache.commons.text.TextStringBuilder;
import org.apache.commons.text.WordUtils;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import net.signbit.samx.AttributeVisitor;
import net.signbit.samx.Parser;
import net.signbit.samx.RendererVisitor;
import net.signbit.samx.parser.SamXParser;

public class CppVisitor extends RendererVisitor
{
   private final STGroup cppGroup;

   private final ArrayList<String> enumerations = new ArrayList<>();
   private final ArrayList<String> bitFields = new ArrayList<>();
   private final ArrayList<String> structures = new ArrayList<>();
   private String namespace = "";
   private String outputName;

   private final HashMap<String, Integer> unitWidths = new HashMap<>();

   private class StructureMember
   {
      final int unitWidth;

      final int unitOffset;
      final int width;
      final int bitOffset;
      final String type;
      final String field;
      final String name;
      final String description;

      private int getInt(SamXParser.RecordDataContext rdc, int index)
      {
         final SamXParser.FlowContext fc = rdc.optionalFlow(index).flow();
         if (fc != null)
         {
            return Integer.parseInt(fc.getText());
         }
         else
         {
            return -1;
         }
      }

      private String getString(SamXParser.RecordDataContext rdc, int index)
      {
         final SamXParser.FlowContext fc = rdc.optionalFlow(index).flow();
         if (fc != null)
         {
            return getPlainText(fc);
         }
         else
         {
            return "";
         }
      }

      private String getBitFieldName(SamXParser.RecordDataContext rdc, int index)
      {
         final SamXParser.FlowContext fc = rdc.optionalFlow(index).flow();
         AttributeVisitor visitor = new AttributeVisitor();
         visitor.visit(fc);
         return visitor.getReference();
      }

      public StructureMember(SamXParser.RecordDataContext rdc, int bitOffset, int unitWidth)
      {
         this.unitWidth = unitWidth;

         unitOffset = getInt(rdc, 0);
         width = getInt(rdc, 1);

         final String localType = getString(rdc, 2);
         if ("bitfield".equals(localType))
         {
            type = getBitFieldName(rdc, 2);
         }
         else
         {
            type = localType;
         }

         field = getString(rdc, 3);
         name = getString(rdc, 4);
         description = getString(rdc, 5);

         this.bitOffset = bitOffset;
      }

      public String getName()
      {
         return name;
      }

      public boolean isNative()
      {
         return "unsigned".equals(type);
      }

      public String getType()
      {
         if (isNative())
         {
            return String.format("uint%d_t", unitWidth);
         }
         else
         {
            return type;
         }
      }

      public String getField()
      {
         return WordUtils.capitalize(field);
      }

      public String getDescription()
      {
         String wrapped = WordUtils.wrap(description, 60);
         StringTokenizer tokenizer = new StringTokenizer(wrapped, '\n');
         TextStringBuilder builder = new TextStringBuilder();
         builder.appendWithSeparators(tokenizer.getTokenList(), "\n    * ");
         return builder.build();
      }

      public int getWidth()
      {
         return width;
      }

      public int getUnitOffset()
      {
         return unitOffset;
      }

      public int getBitOffset()
      {
         return bitOffset;
      }
   }

   private ArrayList<StructureMember> structureMembers = new ArrayList<>();

   public CppVisitor(Writer aWriter, HashMap<String, Parser.Result> docDict, HashMap<String, IOException> errDict, HashMap<String, String> referenceDict, BufferedTokenStream tokenStream)
   {
      super(aWriter, docDict, errDict, referenceDict, tokenStream);

      cppGroup = new STGroupFile("net/signbit/samx/literate/cpp_header.stg");

      unitWidths.put("_8_bit", 8);
      unitWidths.put("_16_bit", 16);
      unitWidths.put("_32_bit", 32);
      unitWidths.put("_64_bit", 64);
   }

   @Override
   public Object visitDocument(SamXParser.DocumentContext ctx)
   {
      ST document = cppGroup.getInstanceOf("/document");

      for (SamXParser.BlockContext bc : ctx.block())
      {
         visit(bc);
      }

      document.add("namespace", namespace);
      document.add("filename", FilenameUtils.getBaseName(outputName));
      document.add("guard", FilenameUtils.getBaseName(outputName).toUpperCase());
      document.add("enumerations", enumerations);
      document.add("bitFields", bitFields);
      document.add("structures", structures);
      document.add("trueFlags", trueFlags);
      document.add("falseFlags", falseFlags);
      append(document.render());

      return null;
   }

   @Override
   public Object visitTypedBlock(SamXParser.TypedBlockContext ctx)
   {
      for (SamXParser.BlockContext bc : ctx.block())
      {
         visit(bc);
      }

      return null;
   }

   @Override
   public Object visitRecordSet(SamXParser.RecordSetContext ctx)
   {
      final String recordType = ctx.NAME().getText();
      if ("structure".equals(recordType))
      {
         renderStructure(ctx);
      }
      else if ("bitfield".equals(recordType))
      {
         renderBitfield(ctx);
      }

      return null;
   }

   private void renderStructure(SamXParser.RecordSetContext ctx)
   {
      structureMembers = new ArrayList<>();

      int dwordCount = 0;
      int bitOffset = 0;

      final String unitWidthHeader = ctx.headerRow().NAME(0).getText();
      final int unitWidth = unitWidths.getOrDefault(unitWidthHeader, 0);

      for (SamXParser.RecordRowContext rrc : ctx.recordRow())
      {
         final SamXParser.RecordDataContext rdc = rrc.recordData();
         if (rdc != null)
         {
            if (isDisabled(rdc.condition()))
            {
               continue;
            }

            StructureMember sm = new StructureMember(rdc, bitOffset, unitWidth);

            bitOffset += sm.width;
            if (bitOffset == 32)
            {
               bitOffset = 0;
            }

            structureMembers.add(sm);
            if (sm.unitOffset > dwordCount)
            {
               dwordCount = sm.unitOffset;
            }
         }
      }

      ST structure = cppGroup.getInstanceOf("/structure");
      AttributeVisitor attributes = getAttributes(ctx);

      structure.add("name", attributes.getId());
      structure.add("unitWidth", unitWidth);
      structure.add("description", getPlainText(ctx.description));
      structure.add("fields", structureMembers);
      structure.add("size", dwordCount + 1);

      structures.add(structure.render());
   }

   private void renderBitfield(SamXParser.RecordSetContext ctx)
   {
      final String unitWidthHeader = ctx.headerRow().NAME(0).getText();
      final int unitWidth = unitWidths.getOrDefault(unitWidthHeader, 0);

      ST template = cppGroup.getInstanceOf("/bitField");
      AttributeVisitor attributes = getAttributes(ctx);

      template.add("name", attributes.getId());
      template.add("unitWidth", unitWidth);
      template.add("description", getPlainText(ctx.description));
      template.add("fields", structureMembers);

      bitFields.add(template.render());
   }

   public void setNamespace(String namespace)
   {
      this.namespace = namespace;
   }

   public void setOutputName(String output)
   {
      this.outputName = output;
   }
}
