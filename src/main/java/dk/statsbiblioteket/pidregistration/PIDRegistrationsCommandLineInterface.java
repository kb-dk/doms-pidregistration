package dk.statsbiblioteket.pidregistration;

import dk.statsbiblioteket.pidregistration.configuration.PropertyBasedRegistrarConfiguration;
import dk.statsbiblioteket.pidregistration.doms.DOMSClient;
import dk.statsbiblioteket.pidregistration.handlesystem.GlobalHandleRegistry;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Entry point for batch job. Responsible for parsing command line arguments
 *
 */
public class PIDRegistrationsCommandLineInterface {

    private static final Logger log = LoggerFactory.getLogger(PIDRegistrationsCommandLineInterface.class);

    public static void main(String[] args) {
        try {
            CommandLine line = parseOptions(args);
            if (line == null) {
                System.exit(1);
            }

            PropertyBasedRegistrarConfiguration config = new PropertyBasedRegistrarConfiguration(
                    new File(System.getProperty("user.home"), "doms-pidregistration.properties"));

            Integer numberOfObjectsToTest = line.hasOption("t") ? Integer.parseInt(line.getOptionValue("t")) : null;

            PIDRegistrations pidRegistrations = new PIDRegistrations(
                    config,
                    new DOMSClient(config),
                    new GlobalHandleRegistry(config),
                    numberOfObjectsToTest);

            pidRegistrations.doRegistrations();
        } catch (Exception e) {
            System.err.println("Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            log.error("Error: {}", e);
            System.exit(2);
        }
    }

    /**
     * Parse arguments. --help will print usage, so will any wrong supplied
     * arguments.
     *
     * @param args Arguments given on command lines.
     * @return Parsed command line. Returns null on errors, in which case a help
     *         message has been printed. Calling method is encouraged to exit.
     */
    public static CommandLine parseOptions(String[] args) {
        CommandLine line;
        Option help = new Option("h", "help", false, "Print this message");
        Option testOption = new Option("t", "test", true, "The number of objects per collection to use in test");
        Options options = new Options();
        options.addOption(help);
        options.addOption(testOption);

        CommandLineParser parser = new PosixParser();
        try {
            line = parser.parse(options, args);
            if (line.hasOption("h")) {
                new HelpFormatter().printHelp("handleregistrationtool.sh", options);
                return null;
            }
        } catch (Exception e) {
            System.out.println("Unable to parse command line arguments: " + e
                    .getMessage());
            new HelpFormatter().printHelp("HandleRegistrationTool", options);
            return null;
        }
        return line;
    }
}
