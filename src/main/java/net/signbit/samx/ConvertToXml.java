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
import java.io.IOException;
import java.io.Writer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

public final class ConvertToXml extends Renderer
{

   private XmlTextVisitor visitor;

   public static void main(String[] args) throws IOException
   {
      ConvertToXml converter = new ConvertToXml();

      converter.render(args);
   }

   @Override
   protected void addCustomOptions(Options options)
   {
      Option rootElement = new Option("r", "root", true, "root element tag (default document)");
      options.addOption(rootElement);

      Option generateDocbook = new Option("b", "docbook", false, "generate DocBook tags");
      options.addOption(generateDocbook);

      Option generateDita = new Option("d", "dita", false, "generate DITA tags");
      options.addOption(generateDita);

      Option rootElementNamespace = new Option("n", "namespace", true, "root element namespace (default null)");
      options.addOption(rootElementNamespace);

      Option rootElementVersion = new Option("v", "version", true, "root element version (default null)");
      options.addOption(rootElementVersion);
   }

   @Override
   protected RendererVisitor makeVisitor(Writer writer, Parser.Result result)
   {
      visitor = new XmlTextVisitor(writer, result.includedDocuments, result.includedExceptions, result.referencePaths, result.tokens);
      return visitor;
   }

   @Override
   protected void addCustomOptions(CommandLine cmd)
   {
      if (cmd.getOptionValue("r") != null)
      {
         visitor.setTopElement(cmd.getOptionValue("r"));

         visitor.setTopElementNamespace(cmd.getOptionValue("n"));
         visitor.setTopElementVersion(cmd.getOptionValue("v"));
      }

      if (cmd.hasOption("d") && cmd.hasOption("b"))
      {
          System.err.println("DocBook and DITA formats are incompatible. Please select only one");
          throw new RuntimeException("Invalid option combination");
      }

      if (cmd.hasOption("b"))
      {
          System.err.println("enable DocBook mode");
          visitor.setDocBookMode();
      }

      if (cmd.hasOption("d"))
      {
          System.err.println("enable DITA mode");
          visitor.setDitaMode();
      }
   }

   @Override
   protected void performCheck(CommandLine cmd)
   {
      checkWellFormed(new InputSource(cmd.getOptionValue("output")));
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
