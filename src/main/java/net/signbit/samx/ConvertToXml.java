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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.cli.*;

public final class ConvertToXml
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

      Option property = new Option("V", true, "variables");
      property.setArgs(2);
      property.setValueSeparator('=');
      options.addOption(property);

      Option trueFlags = new Option("T", "true", true, "flags with true values");
      options.addOption(trueFlags);

      Option falseFlags = new Option("F", "false", true, "flags with false values");
      options.addOption(falseFlags);

      CommandLineParser cmdLine = new DefaultParser();
      HelpFormatter helpFmt = new HelpFormatter();

      try
      {
         CommandLine cmd = cmdLine.parse(options, args);


         Parser.Result result = Parser.parse(cmd.getOptionValue("input"));

         FileWriter fileWriter = new FileWriter(cmd.getOptionValue("output"));

         BufferedWriter writer = new BufferedWriter(fileWriter);

         XmlTextVisitor visitor = new XmlTextVisitor(writer, result.includedDocuments, result.includedExceptions, result.referencePaths);

         Properties props = cmd.getOptionProperties("V");
         visitor.setProperties(props);
         visitor.setTrueFlags(cmd.getOptionValues("T"));
         visitor.setFalseFlags(cmd.getOptionValues("F"));

         visitor.visit(result.document);

         writer.close();
         fileWriter.close();
      }
      catch (ParseException pe)
      {
         System.out.println(pe.getMessage());
         helpFmt.printHelp("ConvertToXml", options);
      }
   }

}
