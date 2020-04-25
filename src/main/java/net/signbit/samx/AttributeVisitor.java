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

import java.util.HashSet;

import net.signbit.samx.parser.SamXParser;
import net.signbit.samx.parser.SamXParserBaseVisitor;

public class AttributeVisitor extends SamXParserBaseVisitor<Void>
{
   private final HashSet<String> classAttributes = new HashSet<>();
   private boolean docBookMode = false;
   private boolean ditaMode = false;

   public String getId()
   {
      return idAttribute;
   }

   public String getName()
   {
      return nameAttribute;
   }

   public String getReference()
   {
      return referenceAttribute;
   }

   private String idAttribute = null;
   private String nameAttribute = null;
   private String referenceAttribute = null;
   private String citationAttribute = null;

   @Override
   public Void visitClassAttr(SamXParser.ClassAttrContext ctx)
   {
      final String newName = ctx.NAME().getText();

      if (classAttributes.contains(newName))
      {
         // throw invalid duplicate class name
      }
      else
      {
         classAttributes.add(newName);
      }

      return null;
   }

   @Override
   public Void visitIdentifierAttr(SamXParser.IdentifierAttrContext ctx)
   {
      idAttribute = ctx.NAME().getText();
      return null;
   }

   @Override
   public Void visitNameAttr(SamXParser.NameAttrContext ctx)
   {
      nameAttribute = ctx.NAME().getText();
      return null;
   }

   @Override
   public Void visitReferenceAttr(SamXParser.ReferenceAttrContext ctx)
   {
      referenceAttribute = ctx.NAME().getText();
      return null;
   }

   @Override
   public Void visitCitationAttr(SamXParser.CitationAttrContext ctx)
   {
      citationAttribute = ctx.text().getText();
      return null;
   }

   @Override
   public String toString()
   {
      StringBuilder builder = new StringBuilder();

      boolean isEmpty = true;

      if ((! classAttributes.isEmpty()) || (idAttribute != null) || (nameAttribute != null))
      {
         builder.append(' ');
      }

      if (! classAttributes.isEmpty())
      {
         if (docBookMode)
         {
            builder.append("role=\"");
         }
         else
         {
            builder.append("class=\"");
         }
         boolean firstElement = true;
         for (String className : classAttributes)
         {
            if (firstElement)
            {
               firstElement = false;
            }
            else
            {
               builder.append(' ');
            }
            builder.append(className);
         }
         builder.append("\"");

         isEmpty = false;
      }

      if (idAttribute != null)
      {
         if (! isEmpty)
         {
            builder.append(' ');
         }

         if (docBookMode)
         {
            builder.append("xml:id=\"");
         }
         else
         {
            builder.append("id=\"");
         }
         builder.append(idAttribute);
         builder.append("\"");

         isEmpty = false;
      }

      if (nameAttribute != null)
      {
         if (! isEmpty)
         {
            builder.append(' ');
         }

         builder.append("name=\"");
         builder.append(nameAttribute);
         builder.append("\"");
      }

      return builder.toString();
   }

   public void setDocBookMode()
   {
      docBookMode = true;
   }

   public void setDitaMode()
   {
      ditaMode = true;
   }

   private StringBuilder visitAttribute(char sigil, String text)
   {
      StringBuilder builder = new StringBuilder();

      builder.append('(');
      builder.append(sigil);
      builder.append(text);
      builder.append(')');

      return builder;
   }

   public String toPlainString()
   {
      StringBuilder builder = new StringBuilder();

      boolean isEmpty = true;

      if (idAttribute != null)
      {
         builder.append(visitAttribute('#', idAttribute));
      }

      if (nameAttribute != null)
      {
         builder.append(visitAttribute('*', nameAttribute));
      }

      for (String className : classAttributes)
      {
         builder.append(visitAttribute('.', className));
      }

      return builder.toString();
   }

}
