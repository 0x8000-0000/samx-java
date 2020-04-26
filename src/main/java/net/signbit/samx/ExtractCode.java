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

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.*;

import net.signbit.samx.visitors.EmbeddedCodeVisitor;

public class ExtractCode
{
   public static void main(String[] args) throws IOException
   {
      Options options = new Options();

      Option input = new Option("i", "input", true, "input file path");
      input.setRequired(true);
      options.addOption(input);

      Option output = new Option("o", "output", true, "output file path");
      output.setRequired(true);
      options.addOption(output);

      CommandLineParser cmdLine = new DefaultParser();
      HelpFormatter helpFmt = new HelpFormatter();

      try
      {
         CommandLine cmd = cmdLine.parse(options, args);

         Parser.Result result = Parser.parse(cmd.getOptionValue("input"));
         if (result.errorCount > 0)
         {
            System.err.print("Failed to parse input file " + cmd.getOptionValue("input"));
            System.exit(10);
         }

         File outputDir = null;
         if (cmd.getOptionValue("output") != null)
         {
            outputDir = new File(cmd.getOptionValue("output"));
            if (! outputDir.isDirectory())
            {
               System.err.println("Specified output path " + cmd.getOptionValue("output") + " is not a directory.");
               System.exit(5);
            }
         }
         else
         {
            outputDir = new File(".");
         }

         EmbeddedCodeVisitor visitor = new EmbeddedCodeVisitor(result.tokens, outputDir);

         visitor.visit(result.document);

         System.exit(0);
      }
      catch (ParseException pe)
      {
         System.err.println(pe.getMessage());
         helpFmt.printHelp("Renderer", options);
      }

      System.exit(1);
   }
}
