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
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.StringTokenizer;
import org.apache.commons.text.TextStringBuilder;
import org.apache.commons.text.WordUtils;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import net.signbit.samx.Parser;
import net.signbit.samx.parser.SamXParser;
import net.signbit.samx.visitors.AttributeVisitor;
import net.signbit.samx.visitors.PlainTextVisitor;
import net.signbit.samx.visitors.RecordSetVisitor;
import net.signbit.samx.visitors.RendererVisitor;

public class CppVisitor extends RendererVisitor
{
   private final STGroup cppGroup;
   private final PlainTextVisitor plainTextVisitor;

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
            return - 1;
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

      public StructureMember(SamXParser.RecordDataContext rdc, int unitOffset, int bitOffset, int unitWidth)
      {
         this.unitWidth = unitWidth;

         int localUnitOffset = getInt(rdc, 0);
         if (localUnitOffset == -1)
         {
            this.unitOffset = unitOffset;
         }
         else
         {
            this.unitOffset = localUnitOffset;
         }
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

      public boolean isWord() { return (unitWidth == width) && (bitOffset == 0); };

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

      plainTextVisitor = new PlainTextVisitor(tokenStream);
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
         renderBitField(ctx);
      }

      return null;
   }

   class StructureDefinition
   {
      final int unitWidth;
      final ArrayList<StructureMember> fields = new ArrayList<>();
      final int dwordCount;

      public StructureDefinition(SamXParser.RecordSetContext ctx)
      {
         RecordSetVisitor visitor = new RecordSetVisitor(getTokenStream());
         RecordSetVisitor.RecordSet rs = (RecordSetVisitor.RecordSet) visitor.visitRecordSet(ctx);

         final String unitWidthHeader = rs.getHeader().getAttributes().get(0);
         unitWidth = unitWidths.getOrDefault(unitWidthHeader, 0);

         int unitCount = 0;
         int bitOffset = 0;

         for (SamXParser.RecordRowContext rrc : ctx.recordRow())
         {
            final SamXParser.RecordDataContext rdc = rrc.recordData();
            if (rdc != null)
            {
               if (isDisabled(rdc.condition()))
               {
                  continue;
               }

               StructureMember sm = new StructureMember(rdc, unitCount, bitOffset, unitWidth);

               bitOffset += sm.width;
               if (bitOffset == 32)
               {
                  bitOffset = 0;
               }

               structureMembers.add(sm);
               if (sm.unitOffset > unitCount)
               {
                  unitCount = sm.unitOffset;
               }
            }
         }

         this.dwordCount = unitCount;
      }
   }


   private void renderStructure(SamXParser.RecordSetContext ctx)
   {
      StructureDefinition sd = new StructureDefinition(ctx);

      ST structure = cppGroup.getInstanceOf("/structure");
      AttributeVisitor attributes = getAttributes(ctx.blockMetadata().metadata());

      structure.add("name", attributes.getId());
      structure.add("unitWidth", sd.unitWidth);
      structure.add("description", getPlainText(ctx.blockMetadata().description));
      structure.add("fields", structureMembers);
      structure.add("size", sd.dwordCount + 1);

      structures.add(structure.render());

      // reset structure
      structureMembers = new ArrayList<>();
   }

   private enum FieldIndices
   {
      Word,
      Offset,
      Width,
      Name,
      Value,
      ValueName,
      ValueDescription
   }

   class EnumerationValue
   {
      String name;
      String value;
      final String description;

      public EnumerationValue(String name, String value, String description)
      {
         if ((name == null) || name.isEmpty())
         {
            this.name = computeNameFromDescription(description);
         }
         else
         {
            this.name = name;
         }
         if (NumberUtils.isCreatable(value))
         {
            this.value = value;
         }
         else
         {
            if ((value.charAt(0) == '0') && (value.charAt(1) == 'b'))
            {
               int intValue = Integer.parseInt(value.substring(2), 2);
               this.value = Integer.toHexString(intValue);
            }
            else
            {
               int intValue = NumberUtils.createInteger(value);
               this.value = Integer.toString(intValue);
            }
         }

         this.description = description + ": " + value;
      }

      public String getName()
      {
         return name;
      }

      public String getValue()
      {
         return value;
      }

      public String getDescription()
      {
         return description;
      }
   }

   private String computeNameFromDescription(String description)
   {
      StringBuilder sb = new StringBuilder();
      for (char ch: description.toCharArray())
      {
         switch (ch)
         {
            case ' ':
            case '/':
               sb.append('_');
               break;

            case '\'':
            case '.':
               break;

            default:
               sb.append(ch);
               break;
         }
      }
      return sb.toString();
   }

   class BitField
   {
      final int word;
      final int offset;
      final int width;
      final String name;

      final String enumType;
      final ArrayList<EnumerationValue> values = new ArrayList<>();

      public BitField(RecordSetVisitor.RecordDataGroup rdg)
      {
         final String wordText = rdg.getValue(FieldIndices.Word.ordinal(), plainTextVisitor);
         word = NumberUtils.createInteger(wordText);
         final String offsetText = rdg.getValue(FieldIndices.Offset.ordinal(), plainTextVisitor);
         offset = NumberUtils.createInteger(offsetText);
         final String widthText = rdg.getValue(FieldIndices.Width.ordinal(), plainTextVisitor);
         width = NumberUtils.createInteger(widthText);
         name = rdg.getValue(FieldIndices.Name.ordinal(), plainTextVisitor);

         if (rdg.getRows().size() > 1)
         {
            enumType = name;

            for (RecordSetVisitor.RecordData rd : rdg.getRows())
            {
               values.add(new EnumerationValue(rd.getValue(FieldIndices.ValueName.ordinal(), plainTextVisitor), rd.getValue(FieldIndices.Value.ordinal(), plainTextVisitor), rd.getValue(FieldIndices.ValueDescription.ordinal(), plainTextVisitor)));
            }

            int maxNameLength = 0;
            int maxValueLength = 0;

            for (EnumerationValue ev: values)
            {
               if (maxNameLength < ev.name.length())
               {
                  maxNameLength = ev.name.length();
               }

               if (maxValueLength < ev.value.length())
               {
                  maxValueLength = ev.value.length();
               }
            }

            for (EnumerationValue ev: values)
            {
               ev.name = String.format("%1$-" + maxNameLength + "s", ev.name);
               ev.value = String.format("%1$" + maxValueLength + "s", ev.value);
            }
         }
         else
         {
            enumType = null;
         }
      }

      public BitField(RecordSetVisitor.RecordData rd)
      {
         final String wordText = rd.getValue(FieldIndices.Word.ordinal(), plainTextVisitor);
         word = NumberUtils.createInteger(wordText);
         final String offsetText = rd.getValue(FieldIndices.Offset.ordinal(), plainTextVisitor);
         offset = NumberUtils.createInteger(offsetText);
         final String widthText = rd.getValue(FieldIndices.Width.ordinal(), plainTextVisitor);
         width = NumberUtils.createInteger(widthText);
         name = rd.getValue(FieldIndices.Name.ordinal(), plainTextVisitor);

         enumType = null;
      }

      public int getWord()
      {
         return word;
      }

      public int getOffset()
      {
         return offset;
      }

      public int getWidth()
      {
         return width;
      }

      public boolean isBoolean()
      {
         return width == 1;
      }

      public boolean isEnumeration()
      {
         return (! values.isEmpty());
      }

      public String getEnumType()
      {
         return enumType;
      }

      public ArrayList<EnumerationValue> getEnumerationValues()
      {
         return values;
      }

      public String getName()
      {
         return name;
      }
   }

   class BitFieldDefinition
   {
      final int unitWidth;
      final ArrayList<BitField> fields = new ArrayList<>();

      public BitFieldDefinition(SamXParser.RecordSetContext ctx)
      {
         RecordSetVisitor visitor = new RecordSetVisitor(getTokenStream());
         RecordSetVisitor.RecordSet rs = (RecordSetVisitor.RecordSet) visitor.visitRecordSet(ctx);

         final String unitWidthHeader = rs.getHeader().getAttributes().get(0);
         unitWidth = unitWidths.getOrDefault(unitWidthHeader, 0);

         for (RecordSetVisitor.RecordDataGroup rdg : rs.getGroups())
         {
            if (rdg.hasSingleValue(FieldIndices.Name.ordinal()) && rdg.hasSingleValue(FieldIndices.Offset.ordinal()) && rdg.hasSingleValue(FieldIndices.Offset.ordinal()))
            {
               fields.add(new BitField(rdg));
            }
            else
            {
               for (RecordSetVisitor.RecordData rd : rdg.getRows())
               {
                  fields.add(new BitField(rd));
               }
            }
         }
      }
   }

   private void renderBitField(SamXParser.RecordSetContext ctx)
   {
      BitFieldDefinition bfd = new BitFieldDefinition(ctx);

      ST template = cppGroup.getInstanceOf("/bitFieldType");
      AttributeVisitor attributes = getAttributes(ctx.blockMetadata().metadata());

      template.add("name", attributes.getId());
      template.add("unitWidth", bfd.unitWidth);
      template.add("description", getPlainText(ctx.blockMetadata().description));
      template.add("fields", bfd.fields);

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
