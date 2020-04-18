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
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import net.signbit.samx.AttributeVisitor;
import net.signbit.samx.Parser;
import net.signbit.samx.RendererVisitor;
import net.signbit.samx.parser.SamXParser;

public class CppVisitor extends RendererVisitor
{
   private STGroup cppGroup;

   private ArrayList<String> enumerations = new ArrayList<>();
   private ArrayList<String> structures = new ArrayList<>();
   private String namespace = "";
   private String outputName;

   public CppVisitor(Writer aWriter, HashMap<String, Parser.Result> docDict, HashMap<String, IOException> errDict, HashMap<String, String> referenceDict, BufferedTokenStream tokenStream)
   {
      super(aWriter, docDict, errDict, referenceDict, tokenStream);

      cppGroup = new STGroupFile("net/signbit/samx/literate/cpp_header.stg");
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
      document.add("structures", structures);
      append(document.render());

      return null;
   }

   @Override
   public Object visitTypedBlock(SamXParser.TypedBlockContext ctx)
   {
      for (SamXParser.BlockContext bc: ctx.block())
      {
         visit(bc);
      }

      return null;
   }

   @Override
   public Object visitRecordSet(SamXParser.RecordSetContext ctx)
   {
      final String recordType = ctx.NAME().getText();
      if (recordType.equals("structure"))
      {
         renderStructure(ctx);
      }

      return null;
   }

   private void renderStructure(SamXParser.RecordSetContext ctx)
   {
      ST structure = cppGroup.getInstanceOf("/structure");
      AttributeVisitor attributes = getAttributes(ctx);

      structure.add("name", attributes.getId());
      structure.add("description", getPlainText(ctx.description));

      structures.add(structure.render());
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
