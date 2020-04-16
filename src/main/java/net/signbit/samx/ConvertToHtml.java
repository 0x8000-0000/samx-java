package net.signbit.samx;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.cli.*;

public class ConvertToHtml
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

         FileWriter fileWriter = new FileWriter(cmd.getOptionValue("output"));

         BufferedWriter writer = new BufferedWriter(fileWriter);

         Parser.Result result = Parser.parse(cmd.getOptionValue("input"));
         HtmlPrinterVisitor visitor = new HtmlPrinterVisitor(writer, result.includedDocuments, result.includedExceptions, result.referencePaths, result.tokens);

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
         helpFmt.printHelp("ConvertToHtml", options);
      }
   }
}
