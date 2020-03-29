package net.signbit.samx;

import java.io.IOException;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

import net.signbit.samx.parser.SamXLexer;
import net.signbit.samx.parser.SamXParser;

public final class Tokenize
{
   public static void main(String[] args)
   {
      if (args.length < 1)
      {
         System.err.println("No arguments provided");
         return;
      }

      try
      {

         CharStream input = CharStreams.fromFileName(args[0]);

         SamXLexer lexer = new SamXLexer(input);

         CommonTokenStream tokens = new CommonTokenStream(lexer);

         tokens.fill();

         for (Token tok : tokens.getTokens())
         {
            final int tokenType = tok.getType();
            String tokenSymbol = lexer.getVocabulary().getSymbolicName(tokenType);
            if (tokenSymbol == null)
            {
               switch (tokenType)
               {
                  case SamXParser.INDENT:
                     tokenSymbol = "INDENT";
                     break;
                  case SamXParser.DEDENT:
                     tokenSymbol = "DEDENT";
                     break;
                  case SamXParser.END:
                     tokenSymbol = "END";
                     break;
                  case SamXParser.INVALID:
                     tokenSymbol = "INVALID";
                     break;
                  default:
                     tokenSymbol = "¯\\_(ツ)_/¯";
                     break;
               }
            }
            String tokenName = String.format("%-10s", tokenSymbol);

            String tokenText = tok.getText().replace("\n", "\\n");

            System.out.println(String.format("%3d:%3d  %s - >%s<", tok.getLine(), tok.getCharPositionInLine(), tokenName, tokenText));
         }
      }
      catch (IOException ioe)
      {
         System.err.println("Caught i/o exception: " + ioe.getMessage());
      }
      catch (Exception ee)
      {
         System.err.println("Caught exception: " + ee.getMessage());
      }
   }
}
