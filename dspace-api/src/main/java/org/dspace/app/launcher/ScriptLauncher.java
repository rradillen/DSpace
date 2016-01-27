/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.dspace.core.ConfigurationManager;
import org.dspace.servicemanager.DSpaceKernelImpl;
import org.dspace.servicemanager.DSpaceKernelInit;
import org.dspace.services.RequestService;
import org.dspace.utils.DSpace;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

/**
 * A DSpace script launcher.
 *
 * @author Stuart Lewis
 * @author Mark Diggory
 */
public class ScriptLauncher
{
    /** The service manager kernel */
    private static transient DSpaceKernelImpl kernelImpl;


    /**
     * Execute the DSpace script launcher
     *
     * @param args Any parameters required to be passed to the scripts it executes
     */
    public static void main(String[] args)
            throws FileNotFoundException, IOException
    {
        // Check that there is at least one argument


        // Initialise the service manager kernel
        try {
            kernelImpl = DSpaceKernelInit.getKernel(null);

            if (!kernelImpl.isRunning())
            {
                kernelImpl.start(ConfigurationManager.getProperty("dspace.dir"));
            }

            if (args.length < 1)
            {
                System.err.println("You must provide at least one command argument");
                display();
                System.exit(1);
            }
        } catch (Exception e)
        {
            // Failed to start so destroy it and log and throw an exception
            try
            {
                kernelImpl.destroy();
            }
            catch (Exception e1)
            {
                // Nothing to do
            }
            String message = "Failure during filter init: " + e.getMessage();
            System.err.println(message + ":" + e);
            throw new IllegalStateException(message, e);
        }

        // Look up command in the configuration, and execute.
        int status;
        status = runOneCommand(args);

        // Destroy the service kernel if it is still alive
        if (kernelImpl != null)
        {
            kernelImpl.destroy();
            kernelImpl = null;
        }

        System.exit(status);
    }

    /**
     * Recognize and execute a single command.
     * @param doc
     * @param args
     */
    static int runOneCommand(String[] args)
    {
        String request = args[0];

        List<CommandType> commands = getConfig();
        CommandType command = null;
        for (CommandType candidate : commands)
        {
            if (request.equalsIgnoreCase(candidate.getName()))
            {
                command = candidate;
                break;
            }
        }

        if (null == command)
        {
            // The command wasn't found
            System.err.println("Command not found: " + args[0]);
            display();
            return 1;
        }

        // Run each step
        List<StepType> steps = command.getStep();
        for (StepType step : steps)
        {
            // Instantiate the class
            Class target = null;

            // Is it the special case 'dsrun' where the user provides the class name?
            String className;
            if ("dsrun".equals(request))
            {
                if (args.length < 2)
                {
                    System.err.println("Error in launcher.xml: Missing class name");
                    return 1;
                }
                className = args[1];
            }
            else {
                className = step.getClassName();
            }
            try
            {
                target = Class.forName(className,
                                       true,
                                       Thread.currentThread().getContextClassLoader());
            }
            catch (ClassNotFoundException e)
            {
                System.err.println("Error in launcher.xml: Invalid class name: " + className);
                return 1;
            }

            // Strip the leading argument from the args, and add the arguments
            // Set <passargs>false</passargs> if the arguments should not be passed on
            String[] useargs = args.clone();
            Class[] argTypes = {useargs.getClass()};
            boolean passargs = true;
            if ((StringUtils.equals("false",step.getPassuserargs())))
            {
                passargs = false;
            }
            if ((args.length == 1) || (("dsrun".equals(request)) && (args.length == 2)) || (!passargs))
            {
                useargs = new String[0];
            }
            else
            {
                // The number of arguments to ignore
                // If dsrun is the command, ignore the next, as it is the class name not an arg
                int x = 1;
                if ("dsrun".equals(request))
                {
                    x = 2;
                }
                String[] argsnew = new String[useargs.length - x];
                for (int i = x; i < useargs.length; i++)
                {
                    argsnew[i - x] = useargs[i];
                }
                useargs = argsnew;
            }

            // Add any extra properties
            List<String> bits = step.getArgument();
            if (bits != null)
            {
                String[] argsnew = new String[useargs.length + bits.size()];
                int i = 0;
                for (String arg : bits)
                {
                    argsnew[i++] = arg;
                }
                for (; i < bits.size() + useargs.length; i++)
                {
                    argsnew[i] = useargs[i - bits.size()];
                }
                useargs = argsnew;
            }

            // Establish the request service startup
            RequestService requestService = kernelImpl.getServiceManager().getServiceByName(
                    RequestService.class.getName(), RequestService.class);
            if (requestService == null)
            {
                throw new IllegalStateException(
                        "Could not get the DSpace RequestService to start the request transaction");
            }

            // Establish a request related to the current session
            // that will trigger the various request listeners
            requestService.startRequest();

            // Run the main() method
            try
            {
                Object[] arguments = {useargs};

                // Useful for debugging, so left in the code...
                /**System.out.print("About to execute: " + className);
                for (String param : useargs)
                {
                    System.out.print(" " + param);
                }
                System.out.println("");**/

                Method main = target.getMethod("main", argTypes);
                main.invoke(null, arguments);

                // ensure we close out the request (happy request)
                requestService.endRequest(null);
            }
            catch (Exception e)
            {
                // Failure occurred in the request so we destroy it
                requestService.endRequest(e);

                // Exceptions from the script are reported as a 'cause'
                Throwable cause = e.getCause();
                System.err.println("Exception: " + cause.getMessage());
                cause.printStackTrace();
                return 1;
            }
        }

        // Everything completed OK
        return 0;
    }

    /**
     * Load the launcher configuration file
     *
     * @return The XML configuration file Document
     */
    private static List<CommandType> getConfig()
    {
        // Load the launcher configuration file
        String config = ConfigurationManager.getProperty("dspace.dir") +
                        System.getProperty("file.separator") + "config" +
                        System.getProperty("file.separator") + "launcher.xml";
        Document doc = null;
        try
        {
            JAXBContext jaxbContext = JAXBContext.newInstance(CommandsType.class.getPackage().getName());
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            CommandsType commands = ((JAXBElement<CommandsType>)unmarshaller.unmarshal(new File(config))).getValue();
            List<CommandType> composite=new LinkedList<>();
            composite.addAll(new DSpace().getServiceManager().getServicesByType(CommandType.class));
            composite.addAll(commands.getCommand());
            return composite;

        }
        catch (Exception e)
        {
            System.err.println("Unable to load the launcher configuration file: [dspace]/config/launcher.xml");
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        return new LinkedList<>();
    }

    /**
     * Display the commands that the current launcher config file knows about
     */
    private static void display()
    {
        // List all command elements
        List<CommandType> commands = getConfig();

        // Sort the commands by name.
        // We cannot just use commands.sort() because it tries to remove and
        // reinsert Elements within other Elements, and that doesn't work.
        TreeMap<String, CommandType> sortedCommands = new TreeMap<>();
        for (CommandType command : commands)
        {
            sortedCommands.put(command.getName(), command);
        }

        // Display the sorted list
        System.out.println("Usage: dspace [command-name] {parameters}");
        for (CommandType command : sortedCommands.values())
        {
            System.out.println(" - " + command.getName() +
                               ": " + command.getDescription());
        }
    }
}
