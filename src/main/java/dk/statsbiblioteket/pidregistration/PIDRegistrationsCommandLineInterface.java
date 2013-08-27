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
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Entry point for batch job. Responsible for parsing command line arguments
 *
 */
public class PIDRegistrationsCommandLineInterface {

    private static final Logger log = LoggerFactory.getLogger(PIDRegistrationsCommandLineInterface.class);

    private static final SimpleDateFormat YEAR_MONTH_DAY = new SimpleDateFormat("yyyy-MM-dd");

    public static void main(String[] args) {
        try {
            CommandLine line = parseOptions(args);
            if (line == null) {
                System.exit(1);
            }

            String configFile = line.hasOption("c") ? line.getOptionValue("c") : System.getProperty("user.home");

            log.info("Config file: " + configFile);
            log.info("From: " + line.getOptionValue("d"));

            Date fromInclusive = YEAR_MONTH_DAY.parse(line.getOptionValue("d"));

            PropertyBasedRegistrarConfiguration config = new PropertyBasedRegistrarConfiguration(
                    new File(configFile));

            PIDRegistrations pidRegistrations = new PIDRegistrations(
                    config,
                    new DOMSClient(config),
                    new GlobalHandleRegistry(config),
                    fromInclusive);

            pidRegistrations.doRegistrations();
        } catch (Exception e) {
            System.err.println("Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            log.error(e.getMessage());
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
        Option configFileOption = new Option("c", "config-file", true,
                                             "Configuration file. Default is $HOME");
        Option dateOption = new Option("d", "date", true,
                                       "the date (YYYY-MM-DD) to query from (inclusive)");
        dateOption.setRequired(true);
        Options options = new Options();
        options.addOption(help);
        options.addOption(configFileOption);
        options.addOption(dateOption);

        CommandLineParser parser = new PosixParser();
        try {
            line = parser.parse(options, args);
            if (line.hasOption("h")) {
                new HelpFormatter().printHelp("handleregistrationtool.sh", options);
                return null;
            }

            if (!line.hasOption("d")) {
                System.err.println("Missing required arguments");
                new HelpFormatter().printHelp("HandleRegistrationTool", options);
                return null;
            }
            YEAR_MONTH_DAY.parse(line.getOptionValue("d"));
        } catch (Exception e) {
            System.out.println("Unable to parse command line arguments: " + e
                    .getMessage());
            new HelpFormatter().printHelp("HandleRegistrationTool", options);
            return null;
        }
        return line;
    }
}
