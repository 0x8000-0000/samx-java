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
import java.util.Properties;

import org.apache.commons.cli.*;

public abstract class Renderer
{
   public Options makeOptions()
   {
      Options options = new Options();

      Option input = new Option("i", "input", true, "input file path");
      input.setRequired(true);
      options.addOption(input);

      Option output = new Option("o", "output", true, "output file path");
      output.setRequired(true);
      options.addOption(output);

      Option property = new Option("V", true, "variables");
      property.setArgs(2);
      property.setValueSeparator('=');
      options.addOption(property);

      Option trueFlags = new Option("T", "true", true, "flags with true values");
      options.addOption(trueFlags);

      Option falseFlags = new Option("F", "false", true, "flags with false values");
      options.addOption(falseFlags);

      return options;
   }

   protected abstract void addCustomOptions(Options options);

   protected abstract RendererVisitor makeVisitor(Writer writer, Parser.Result result);

   protected abstract void addCustomOptions(CommandLine cmd);

   protected abstract void performCheck(CommandLine cmd) throws FileNotFoundException;

   public void render(String[] args) throws IOException
   {
      Options options = makeOptions();

      addCustomOptions(options);

      CommandLineParser cmdLine = new DefaultParser();
      HelpFormatter helpFmt = new HelpFormatter();

      try
      {
         CommandLine cmd = cmdLine.parse(options, args);

         FileWriter fileWriter = new FileWriter(cmd.getOptionValue("output"));

         BufferedWriter writer = new BufferedWriter(fileWriter);

         Parser.Result result = Parser.parse(cmd.getOptionValue("input"));
         RendererVisitor visitor = makeVisitor(writer, result);

         Properties props = cmd.getOptionProperties("V");
         visitor.setProperties(props);
         visitor.setTrueFlags(cmd.getOptionValues("T"));
         visitor.setFalseFlags(cmd.getOptionValues("F"));

         addCustomOptions(cmd);

         visitor.visit(result.document);

         writer.close();
         fileWriter.close();

         performCheck(cmd);
      }
      catch (ParseException pe)
      {
         System.out.println(pe.getMessage());
         helpFmt.printHelp("Renderer", options);
      }
   }
}
