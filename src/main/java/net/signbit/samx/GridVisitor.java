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

package net.signbit.samx;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.antlr.v4.runtime.BufferedTokenStream;

import net.signbit.samx.parser.SamXParser;
import net.signbit.samx.parser.SamXParserBaseVisitor;
import net.signbit.samx.parser.SamXParserVisitor;

public class GridVisitor extends SamXParserBaseVisitor<StringBuilder>
{
   private final BufferedTokenStream tokenStream;

   public GridVisitor(BufferedTokenStream tokenStream)
   {
      this.tokenStream = tokenStream;
   }

   static class GridCell
   {
      int rowSpan = 1;
      int colSpan = 1;

      final List<SamXParser.AttributeContext> attributes;
      final SamXParser.FlowContext flow;

      GridCell(List<SamXParser.AttributeContext> attributes, SamXParser.OptionalFlowContext optionalFlowContext)
      {
         this.attributes = attributes;
         if (optionalFlowContext != null)
         {
            flow = optionalFlowContext.flow();
         }
         else
         {
            flow = null;
         }
      }

      void setSpan(String span)
      {
         for (char ch : span.toCharArray())
         {
            if (ch == '|')
            {
               colSpan++;
            }
            else if (ch == '-')
            {
               rowSpan++;
            }
            else
            {
               // error
            }
         }

         if (colSpan > 1)
         {
            colSpan--;
         }

         if (rowSpan > 1)
         {
            rowSpan--;
         }
      }

      public String getContent(SamXParserVisitor<StringBuilder> visitor)
      {
         StringBuilder builder = new StringBuilder();
         if (flow != null)
         {
            builder.append(visitor.visitFlow(flow));
         }
         return builder.toString();
      }

      public boolean hasContent()
      {
         return flow != null && (!flow.getText().isEmpty());
      }

      public Object renderContent(SamXParserVisitor<Object> visitor)
      {
         return visitor.visitFlow(flow);
      }

      public String getAttributesPlain()
      {
         AttributeVisitor av = new AttributeVisitor();
         for (SamXParser.AttributeContext ac : attributes)
         {
            av.visit(ac);
         }
         return av.toPlainString();
      }
   }

   static class GeneralGridRow
   {
      final SamXParser.MetadataContext metadataContext;

      final ArrayList<GridCell> cells = new ArrayList<>();

      public int getColumnCount()
      {
         return cells.size();
      }

      GeneralGridRow(SamXParser.GeneralGridRowDataContext rdc)
      {
         metadataContext = rdc.metadata();

         for (SamXParser.GeneralGridElementContext ggec : rdc.generalGridElement())
         {
            if (ggec.gridElement() != null)
            {
               GridVisitor.GridCell gc = new GridVisitor.GridCell(ggec.gridElement().attribute(), ggec.gridElement().optionalFlow());
               cells.add(gc);
            }
            else if (ggec.spanGridElement() != null)
            {
               GridVisitor.GridCell gc = new GridVisitor.GridCell(ggec.spanGridElement().attribute(), ggec.spanGridElement().optionalFlow());
               gc.setSpan(ggec.spanGridElement().MUL_COLSEP().getText());

               for (int ii = 0; ii < gc.colSpan; ++ii)
               {
                  cells.add(gc);
               }
            }
         }
      }
   }

   static class GeneralGridGroup
   {
      ArrayList<GridVisitor.GeneralGridRow> rows = new ArrayList<>();

      int conditionColumnWidth = 0;
      int columnWidths[];

      GeneralGridGroup(SamXParser.GeneralGridGroupContext gggc, SamXParserVisitor<StringBuilder> visitor)
      {
         HashSet<Integer> columnCount = new HashSet<>();
         for (SamXParser.GeneralGridRowContext rc : gggc.generalGridRow())
         {
            final SamXParser.GeneralGridRowDataContext rdc = rc.generalGridRowData();
            if (rdc != null)
            {
               final GridVisitor.GeneralGridRow ggr = new GridVisitor.GeneralGridRow(rdc);

               if (visitor != null)
               {
                  final String attributeCondition = visitor.visitMetadata(ggr.metadataContext).toString();

                  if (conditionColumnWidth < attributeCondition.length())
                  {
                     conditionColumnWidth = attributeCondition.length();
                  }
               }

               rows.add(ggr);
               columnCount.add(ggr.getColumnCount());
            }
         }

         if (columnCount.size() != 1)
         {
            throw new RuntimeException("Invalid table specification: multiple table column sizes: " + columnCount.toString());
         }

         columnWidths = new int[columnCount.iterator().next()];

         if (visitor != null)
         {
            for (GridVisitor.GeneralGridRow ggr : rows)
            {
               for (int ii = 0; ii < ggr.cells.size(); ++ii)
               {
                  final GridVisitor.GridCell gc = ggr.cells.get(ii);
                  final String content = gc.getContent(visitor);

                  int rowSpanIndicator = 0;
                  if (gc.rowSpan > 1)
                  {
                     rowSpanIndicator = gc.rowSpan;
                  }

                  final int thisWidth = (int) Math.ceil((content.length() + rowSpanIndicator) / (double) (gc.colSpan));

                  if (columnWidths[ii] < thisWidth)
                  {
                     columnWidths[ii] = thisWidth;
                  }
               }
            }
         }
      }
   }
}
