package dk.statsbiblioteket.pidregistration;

import dk.statsbiblioteket.pidregistration.doms.DomsHandler;
import dk.statsbiblioteket.pidregistration.handleserver.HandleHandler;
import dk.statsbiblioteket.pidregistration.configuration.PropertyBasedRegistrarConfiguration;
import dk.statsbiblioteket.pidregistration.configuration.RegistrarConfiguration;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;

/**
 * Tool, that given an RDF query that returns a list of objects, an URL
 * pattern and a configuration file registers all objects matching the RDF query
 * as handles resolving to the URL pattern.
 */
public class HandleRegistrarTool {
    private static Log log = LogFactory.getLog(HandleRegistrarTool.class);

    public static void main(String[] args) {
        try {
            CommandLine line = parseOptions(args);
            if (line == null) {
                System.exit(1);
            }

            String query = line.getOptionValue("q");
            String urlPattern = line.getOptionValue("u");
            String configFile;
            if (line.hasOption("c")) {
                configFile = line.getOptionValue("c");
            } else {
                configFile = System.getProperty("user.home")
                        + "/.config/handle/handleregistrar.properties"
                        .replaceAll("/", System.getProperty("file.separator"));
            }

            log.info("Config file: " + configFile);
            log.info("Query: " + query);
            log.info("URL pattern: " + urlPattern);
            RegistrarConfiguration config = new PropertyBasedRegistrarConfiguration(
                    new File(configFile));
            HandleRegistrar registrar = new BasicHandleRegistrar(new DomsHandler(
                                                                         config),
                                                                 new HandleHandler(
                                                                         config));
            System.out.println(registrar.addHandles(query, urlPattern));
        } catch (Exception e) {
            System.err.println("Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            log.error("Failure adding handles", e);
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
                                             "Configuration file. Default is $HOME/.config/handle/handleregistrar.properties");
        Option queryOption = new Option("q", "query", true,
                                        "iTQL query for getting PIDs from DOMS. Must return a list with PIDs as first element.");
        queryOption.setRequired(true);
        Option urlPatternOption = new Option("u", "url-pattern", true,
                                             "URL pattern for what the objects should resolve as. %s is replaced with PID.");
        urlPatternOption.setRequired(true);

        Options options = new Options();
        options.addOption(help);
        options.addOption(configFileOption);
        options.addOption(queryOption);
        options.addOption(urlPatternOption);

        CommandLineParser parser = new PosixParser();
        try {
            line = parser.parse(options, args);
            if (line.hasOption("h")) {
                new HelpFormatter().printHelp("handleregistrartool.sh", options);
                return null;
            }

            if (!line.hasOption("q") || !line.hasOption("u")) {
                System.err.println("Missing required arguments");
                new HelpFormatter().printHelp("HandleRegistrarTool", options);
                return null;
            }
        } catch (ParseException e) {
            System.out.println("Unable to parse command line arguments: " + e
                    .getMessage());
            new HelpFormatter().printHelp("HandleRegistrarTool", options);
            return null;
        }
        return line;
    }
}
