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

import java.io.*;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.w3c.tidy.Tidy;

public class ConvertToHtml extends Renderer
{

   private HtmlPrinterVisitor visitor;

   public static void main(String[] args) throws IOException
   {
      ConvertToHtml converter = new ConvertToHtml();

      converter.render(args);
   }

   @Override
   protected void addCustomOptions(Options options)
   {

   }

   @Override
   protected RendererVisitor makeVisitor(Writer writer, Parser.Result result)
   {
      visitor = new HtmlPrinterVisitor(writer, result.includedDocuments, result.includedExceptions, result.referencePaths, result.tokens);
      return visitor;
   }

   @Override
   protected void addCustomOptions(CommandLine cmd)
   {

   }

   @Override
   protected boolean performCheck(CommandLine cmd) throws FileNotFoundException
   {
      Tidy tidy = new Tidy();
      FileInputStream fis = new FileInputStream(cmd.getOptionValue("output"));
      StringWriter output = new StringWriter();
      tidy.parse(fis, output);
      final int count = tidy.getParseErrors();
      if (count != 0)
      {
         System.err.println(String.format("JTidy found %d errors", count));
         System.err.print(output.toString());
      }

      return (count == 0);
   }
}
