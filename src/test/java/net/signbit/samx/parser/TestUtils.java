package net.signbit.samx.parser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class TestUtils
{
   static String getResourceContents(String resourceName)
   {
      ClassLoader classLoader = ClassLoader.getSystemClassLoader();

      InputStream is = classLoader.getResourceAsStream(resourceName);

      if (is == null)
      {
         throw new RuntimeException("Resource not found");
      }

      InputStreamReader isr = new InputStreamReader(is);
      BufferedReader reader = new BufferedReader(isr);

      return reader.lines().collect(Collectors.joining(System.lineSeparator())) + "\n";
   }
}
