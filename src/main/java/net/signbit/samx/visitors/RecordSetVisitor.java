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

import java.util.ArrayList;

import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.math.NumberUtils;

import net.signbit.samx.parser.SamXParser;
import net.signbit.samx.parser.SamXParserBaseVisitor;

public class RecordSetVisitor extends SamXParserBaseVisitor<RecordSetVisitor.AST>
{
   private final BufferedTokenStream tokenStream;

   public class AST
   {

   }

   class RecordHeader extends AST
   {
      final ArrayList<String> attributes;
      boolean hasTrailingBar = false;

      RecordHeader(SamXParser.HeaderRowContext ctx)
      {
         attributes = new ArrayList<>(ctx.NAME().size());
         for (TerminalNode tn : ctx.NAME())
         {
            attributes.add(tn.getText());
         }

         if (ctx.trailingBar != null)
         {
            hasTrailingBar = true;
         }
      }
   }

   public class RecordData extends AST
   {
      final SamXParser.ConditionContext condition;
      final ArrayList<SamXParser.FlowContext> flows;

      RecordData(SamXParser.RecordDataContext ctx)
      {
         condition = ctx.condition();

         flows = new ArrayList<>(ctx.optionalFlow().size());
         for (SamXParser.OptionalFlowContext ofc : ctx.optionalFlow())
         {
            flows.add(ofc.flow());
         }
      }

      public String getValue(int index, SamXParserBaseVisitor<StringBuilder> visitor)
      {
         SamXParser.FlowContext fc = flows.get(index);
         if (fc != null)
         {
            return visitor.visitFlow(fc).toString();
         }
         else
         {
            return null;
         }
      }
   }

   public class RecordDataGroup extends AST
   {
      final ArrayList<RecordData> rows = new ArrayList<>();
      final int[] nonNullValues;
      final int startLine;

      RecordDataGroup(int attributeCount, int startLine)
      {
         this.startLine = startLine;
         nonNullValues = new int[attributeCount];
      }

      public void closeGroup(int endLine)
      {
         for (RecordData rd : rows)
         {
            for (int ii = 0; ii < nonNullValues.length; ++ ii)
            {
               if (rd.flows.get(ii) != null)
               {
                  nonNullValues[ii]++;
               }
            }
         }

         final int rowCount = rows.size();

         for (int ii = 0; ii < nonNullValues.length; ++ ii)
         {
            if ((nonNullValues[ii] != 0) && (nonNullValues[ii] != 1) && (nonNullValues[ii] != rowCount))
            {
               throw new RuntimeException(String.format("Record set group starting at line %d and ending at line %d has an invalid number of distinct values %d", startLine, endLine, nonNullValues[ii]));
            }
         }
      }

      public boolean hasSingleValue(int index)
      {
         return nonNullValues[index] != rows.size();
      }

      public String getValue(int index, SamXParserBaseVisitor<StringBuilder> visitor)
      {
         if (hasSingleValue(index))
         {
            SamXParser.FlowContext fc = rows.get(0).flows.get(index);
            if (fc != null)
            {
               return visitor.visitFlow(fc).toString();
            }
            else
            {
               return null;
            }
         }
         else
         {
            throw new RuntimeException("Multiple values found");
         }
      }

      public ArrayList<RecordData> getRows()
      {
         return rows;
      }
   }

   public class RecordSet extends AST
   {
      boolean hasHeaderSeparator = false;
      boolean hasBottomBorder = false;

      RecordHeader header;
      final ArrayList<RecordDataGroup> groups = new ArrayList<>();

      int conditionColumnWidth = 0;
      int[] columnWidths;

      boolean[] isInteger;

      void computePresentation(SamXParserBaseVisitor<StringBuilder> visitor)
      {
         final int columnCount = header.attributes.size();
         columnWidths = new int[columnCount];
         isInteger = new boolean[columnCount];

         for (int ii = 0; ii < columnCount; ++ ii)
         {
            columnWidths[ii] = header.attributes.get(ii).length();
            isInteger[ii] = true;
         }

         for (RecordDataGroup rdg : groups)
         {
            for (RecordData rd : rdg.rows)
            {
               for (int ii = 0; ii < columnCount; ++ ii)
               {
                  final SamXParser.FlowContext fc = rd.flows.get(ii);
                  if (fc != null)
                  {
                     final String rendered = visitor.visitFlow(fc).toString();
                     if (columnWidths[ii] < rendered.length())
                     {
                        columnWidths[ii] = rendered.length();
                     }
                     checkNumberFormat(ii, rendered);
                  }
               }

               if (rd.condition != null)
               {
                  final String renderedCondition = visitor.visitCondition(rd.condition).toString();
                  if (conditionColumnWidth < renderedCondition.length())
                  {
                     conditionColumnWidth = renderedCondition.length();
                  }
               }
            }
         }
      }

      private void checkNumberFormat(int ii, String content)
      {
         if (content.isEmpty())
         {
            return;
         }

         if (NumberUtils.isCreatable(content))
         {
            try
            {
               NumberUtils.createInteger(content);
            }
            catch (NumberFormatException nfei)
            {
               isInteger[ii] = false;
            }
         }
         else
         {
            isInteger[ii] = false;
         }
      }

      public ArrayList<RecordDataGroup> getGroups()
      {
         return groups;
      }
   }

   public RecordSetVisitor(BufferedTokenStream tokenStream)
   {
      this.tokenStream = tokenStream;
   }

   @Override
   public AST visitHeaderRow(SamXParser.HeaderRowContext ctx)
   {
      return new RecordHeader(ctx);
   }

   @Override
   public AST visitRecordRow(SamXParser.RecordRowContext ctx)
   {
      if (ctx.recordData() != null)
      {
         return visitRecordData(ctx.recordData());
      }

      return null;
   }

   @Override
   public AST visitRecordData(SamXParser.RecordDataContext ctx)
   {
      return new RecordData(ctx);
   }

   @Override
   public AST visitRecordSet(SamXParser.RecordSetContext ctx)
   {
      RecordSet rs = new RecordSet();

      rs.header = (RecordHeader) visitHeaderRow(ctx.headerRow());

      final int attributeCount = rs.header.attributes.size();

      RecordDataGroup rdg = new RecordDataGroup(attributeCount, ctx.headerRow().stop.getLine());

      for (SamXParser.RecordRowContext rrc : ctx.recordRow())
      {
         RecordData rd = (RecordData) visitRecordRow(rrc);
         if (rd == null)
         {
            // current group is finished, starting a new group
            if (! rdg.rows.isEmpty())
            {
               rdg.closeGroup(rrc.start.getLine());
               rs.groups.add(rdg);
               rdg = new RecordDataGroup(attributeCount, rrc.start.getLine());
            }
            else
            {
               if (rs.groups.isEmpty())
               {
                  rs.hasHeaderSeparator = true;
               }
            }
         }
         else
         {
            rdg.rows.add(rd);
         }
      }

      if (! rdg.rows.isEmpty())
      {
         rdg.closeGroup(ctx.stop.getLine());
         rs.groups.add(rdg);
      }
      else
      {
         rs.hasBottomBorder = true;
      }

      return rs;
   }
}
