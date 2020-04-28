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
      ArrayList<String> columns;

      RecordHeader(SamXParser.HeaderRowContext ctx)
      {
         columns = new ArrayList<>(ctx.NAME().size());
         for (TerminalNode tn: ctx.NAME())
         {
            columns.add(tn.getText());
         }
      }
   }

   class RecordData extends AST
   {
      SamXParser.ConditionContext condition;
      ArrayList<SamXParser.FlowContext> flows;

      RecordData(SamXParser.RecordDataContext ctx)
      {
         condition = ctx.condition();

         flows = new ArrayList<>(ctx.optionalFlow().size());
         for (SamXParser.OptionalFlowContext ofc: ctx.optionalFlow())
         {
            flows.add(ofc.flow());
         }
      }
   }

   class RecordDataGroup extends AST
   {
      ArrayList<RecordData> rows = new ArrayList<>();

   }

   class RecordSet extends AST
   {
      RecordHeader header;
      ArrayList<RecordDataGroup> groups = new ArrayList<>();

      int[] computeColumnWidths(SamXParserBaseVisitor<StringBuilder> visitor)
      {
         final int columnCount = header.columns.size();

         int[] columnWidths = new int[columnCount + 1];

         for (int ii = 0; ii < columnCount; ++ ii)
         {
            columnWidths[ii] = header.columns.get(ii).length();
         }

         for (RecordDataGroup rdg: groups)
         {
            for (RecordData rd: rdg.rows)
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
                  }
               }

               if (rd.condition != null)
               {
                  final String renderedCondition = visitor.visitCondition(rd.condition).toString();
                  if (columnWidths[columnCount] < renderedCondition.length())
                  {
                     columnWidths[columnCount] = renderedCondition.length();
                  }
               }
            }
         }

         return columnWidths;
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

      RecordDataGroup rdg = new RecordDataGroup();

      for (SamXParser.RecordRowContext rrc: ctx.recordRow())
      {
         RecordData rd = (RecordData) visitRecordRow(rrc);
         if (rd == null)
         {
            // current group is finished, starting a new group
            if (! rdg.rows.isEmpty())
            {
               rs.groups.add(rdg);
               rdg = new RecordDataGroup();
            }
         }
         else
         {
            rdg.rows.add(rd);
         }
      }

      if (! rdg.rows.isEmpty())
      {
         rs.groups.add(rdg);
      }

      return rs;
   }
}
