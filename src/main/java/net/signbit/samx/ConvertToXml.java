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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.cli.*;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

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

      Option rootElement = new Option("r", "root", true, "root element tag (default document)");
      options.addOption(rootElement);

      Option rootElementNamespace = new Option("n", "namespace", true, "root element namespace (default null)");
      options.addOption(rootElementNamespace);

      Option rootElementVersion = new Option("v", "version", true, "root element version (default null)");
      options.addOption(rootElementVersion);

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
         XmlTextVisitor visitor = new XmlTextVisitor(writer, result.includedDocuments, result.includedExceptions, result.referencePaths, result.tokens);

         Properties props = cmd.getOptionProperties("V");
         visitor.setProperties(props);
         visitor.setTrueFlags(cmd.getOptionValues("T"));
         visitor.setFalseFlags(cmd.getOptionValues("F"));

         if (cmd.getOptionValue("r") != null)
         {
            visitor.setTopElement(cmd.getOptionValue("r"));

            visitor.setTopElementNamespace(cmd.getOptionValue("n"));
            visitor.setTopElementVersion(cmd.getOptionValue("v"));
         }

         visitor.visit(result.document);

         writer.close();
         fileWriter.close();

         checkWellFormed(new InputSource(cmd.getOptionValue("output")));
      }
      catch (ParseException pe)
      {
         System.out.println(pe.getMessage());
         helpFmt.printHelp("ConvertToXml", options);
      }
   }

   private static class SimpleErrorHandler implements ErrorHandler
   {
      public void warning(SAXParseException e)
      {
         System.err.println(e.getMessage());
      }

      public void error(SAXParseException e)
      {
         System.err.println(e.getMessage());
      }

      public void fatalError(SAXParseException e)
      {
         System.err.println(e.getMessage());
      }
   }

   private static void checkWellFormed(InputSource output)
   {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setValidating(false);
      factory.setNamespaceAware(true);

      try
      {
         DocumentBuilder builder = factory.newDocumentBuilder();

         builder.setErrorHandler(new SimpleErrorHandler());

         Document document = builder.parse(output);

         System.err.println("XML output is well-formed");
      }
      catch (Exception ee)
      {
         ee.printStackTrace(System.err);
      }

   }

}
