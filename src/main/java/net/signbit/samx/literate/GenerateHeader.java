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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import net.signbit.samx.Parser;
import net.signbit.samx.Renderer;
import net.signbit.samx.RendererVisitor;

public class GenerateHeader extends Renderer
{

   private CppVisitor visitor;

   public static void main(String[] args) throws IOException
   {
      GenerateHeader converter = new GenerateHeader();

      converter.render(args);
   }

   @Override
   protected void addCustomOptions(Options options)
   {
      Option namespace = new Option("n", "namespace", true, "namespace");
      options.addOption(namespace);
   }

   @Override
   protected RendererVisitor makeVisitor(Writer writer, Parser.Result result)
   {
      visitor = new CppVisitor(writer, result.includedDocuments, result.includedExceptions, result.referencePaths, result.tokens);
      return visitor;
   }

   @Override
   protected void addCustomOptions(CommandLine cmd)
   {
      visitor.setNamespace(cmd.getOptionValue("namespace"));
      visitor.setOutputName(cmd.getOptionValue("output"));
   }

   @Override
   protected void performCheck(CommandLine cmd) throws FileNotFoundException
   {

   }
}
