package miniJava;

import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class Compiler {
	// Main function, the file to compile will be an argument.
	public static void main(String[] args) {
		// TODO: Instantiate the ErrorReporter object
		ErrorReporter reporter = new ErrorReporter();
		// TODO: Check to make sure a file path is given in args
		if(args.length < 1) {
			System.out.println("No file provided");
			System.exit(1);
		} else {
			try {
				FileInputStream iStream = new FileInputStream(args[0]);
				Scanner scanning = new Scanner(iStream,reporter);
				Parser parsing = new Parser(scanning, reporter);
				parsing.parse();
				if(reporter.hasErrors()) {
					System.out.println("Error");
					reporter.outputErrors();
				} else {
					System.out.println("Success");
				}
			} catch (FileNotFoundException e) {
				System.out.println("File not found");
				System.exit(1);
			}
		}
		// TODO: Create the inputStream using new FileInputStream

		// TODO: Instantiate the scanner with the input stream and error object

		// TODO: Instantiate the parser with the scanner and error object

		// TODO: Call the parser's parse function

		// TODO: Check if any errors exist, if so, println("Error")
		//  then output the errors
		// TODO: If there are no errors, println("Success")
	}
}
