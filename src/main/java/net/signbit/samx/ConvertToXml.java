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
import java.io.*;
import java.util.zip.GZIPInputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.thaiopensource.validate.ValidationDriver;
import net.signbit.samx.visitors.RendererVisitor;
import net.signbit.samx.visitors.XmlTextVisitor;

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

      Option schemaPath = new Option("s", "schema", true, "RelaxNG schema");
      options.addOption(schemaPath);
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
      if (cmd.hasOption("d") && cmd.hasOption("b"))
      {
         System.err.println("DocBook and DITA formats are incompatible. Please select only one");
         throw new RuntimeException("Invalid option combination");
      }

      if (cmd.hasOption("d"))
      {
         System.err.println("Enable DITA mode");
         visitor.setDitaMode();
      }

      if (cmd.hasOption("b"))
      {
         System.err.println("Enable DocBook mode");

         visitor.setDocBookMode();

         // load sensible defaults
         visitor.setTopElement("book");
         visitor.setTopElementNamespace("http://docbook.org/ns/docbook");
         visitor.setTopElementVersion("5.1");
      }

      if (cmd.getOptionValue("r") != null)
      {
         visitor.setTopElement(cmd.getOptionValue("r"));
      }

      if (cmd.getOptionValue("n") != null)
      {
         visitor.setTopElementNamespace(cmd.getOptionValue("n"));
      }

      if (cmd.getOptionValue("v") != null)
      {
         visitor.setTopElementVersion(cmd.getOptionValue("v"));
      }
   }

   private File findSchemaFile(CommandLine cmd)
   {
      File schemaFile = null;

      if (cmd.getOptionValue("schema") != null)
      {
         schemaFile = new File(cmd.getOptionValue("schema"));
         if (schemaFile.exists())
         {
            return schemaFile;
         }
      }

      final File outputFile = new File(cmd.getOptionValue("output"));
      final File outputFileParent = outputFile.getParentFile();
      schemaFile = new File(outputFileParent, "docbook.rng");
      if (schemaFile.exists())
      {
         return schemaFile;
      }

      File inputFile = new File(cmd.getOptionValue("input"));
      File inputFileParent = inputFile.getParentFile();
      schemaFile = new File(inputFileParent, "docbook.rng");
      if (schemaFile.exists())
      {
         return schemaFile;
      }

      return null;
   }

   @Override
   protected boolean performCheck(CommandLine cmd) throws IOException, SAXException
   {
      if (checkWellFormed(new InputSource(cmd.getOptionValue("output"))))
      {
         if (cmd.hasOption("b"))
         {
            final File schemaFile = findSchemaFile(cmd);
            if (schemaFile == null)
            {
               System.err.println("Could not find the schema file near the input or output.");
               System.err.println("You can download it from https://docbook.org/xml/5.1/rng/docbook.rng");

               return false;
            }

            final ValidationDriver vd = new ValidationDriver();

            InputStream schemaStream = new FileInputStream(schemaFile);
            if (schemaFile.getName().endsWith(".gz"))
            {
               schemaStream = new GZIPInputStream(schemaStream);
            }

            vd.loadSchema(new InputSource(schemaStream));

            final boolean isValid = vd.validate(new InputSource(cmd.getOptionValue("output")));

            if (isValid)
            {
               System.err.println("DocBook document validated using Jing");
            }
            else
            {
               System.err.println("DocBook document failed to validate");
            }

            return isValid;
         }
      }

      return false;
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

   private static boolean checkWellFormed(InputSource output)
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

         return true;
      }
      catch (Exception ee)
      {
         ee.printStackTrace(System.err);
         return false;
      }
   }

}
