/*******************************************************************************
 * Copyright (c) 2012 KDM Analytics, Inc. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Open
 * Source Initiative OSI - Open Software License v3.0 which accompanies this
 * distribution, and is available at
 * http://www.opensource.org/licenses/osl-3.0.php/
 ******************************************************************************/
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Scanner;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import com.kdmanalytics.toif.framework.parser.StreamGobbler;
import com.kdmanalytics.toif.framework.toolAdaptor.AbstractAdaptor;
import com.kdmanalytics.toif.framework.toolAdaptor.AdaptorOptions;
import com.kdmanalytics.toif.framework.utils.FindingCreator;
import com.kdmanalytics.toif.framework.xmlElements.entities.Element;

public class SplintAdaptor extends AbstractAdaptor
{
    
    private String path = null;
    
    private File tmpFile = null;
    
    /**
     * return this adaptors name
     */
    /*
     * (non-Javadoc)
     * 
     * @see
     * com.kdmanalytics.toif.framework.toolAdaptor.AbstractAdaptor#getAdaptorName
     * ()
     */
    @Override
    public String getAdaptorName()
    {
        return "Splint Adaptor";
    }
    
    /**
     * return the adaptors description.
     */
    /*
     * (non-Javadoc)
     * 
     * @see com.kdmanalytics.toif.framework.toolAdaptor.AbstractAdaptor#
     * getAdaptorDescription()
     */
    @Override
    public String getAdaptorDescription()
    {
        return "Splint is a tool for statically checking C programs for security vulnerabilities and common programming mistakes. With minimal effort, splint can be used as a better lint. If additional effort is invested adding annotations to programs, splint can perform stronger checks than can be done by any standard lint.";
    }
    
    /**
     * return the adaptors version.
     */
    @Override
    public String getAdaptorVersion()
    {
        return "0.5";
    }
    
    /**
     * Create the commands to run the tool.
     */
    @Override
    public String[] runToolCommands(AdaptorOptions options, String[] otherOpts)
    {
        // we need the path in order to construct the full file path.
        path = options.getInputFile().getParent();
        
        // this is where the output of the tool is going to be written in order
        // for us to collect it.
        tmpFile = new File(System.getProperty("java.io.tmpdir"), "Splint" + options.getInputFile().getName());
        tmpFile.deleteOnExit();
        // the required commands to run the tool.
        final String[] commands = { "splint", "-weak", options.getInputFile().toString(), "+csv", tmpFile.getPath(), "+csvoverwrite" };
        
        String[] allCommands = (String[]) ArrayUtils.addAll(commands, otherOpts);
        
        return allCommands;
    }
    
    @Override
    public ArrayList<Element> parse(Process process, AdaptorOptions options, com.kdmanalytics.toif.framework.xmlElements.entities.File file)
    {
        // new finding creator
        final FindingCreator creator = new FindingCreator(getProperties(), getAdaptorName());
        
        // get the stream from the process.
        final InputStream inStream = process.getInputStream();
        InputStream errStream = process.getErrorStream();
        
        Thread stderr = new Thread(new StreamGobbler(errStream, null));
        stderr.start();
        
        try
        {
            stderr.join();
        }
        catch (InterruptedException e1)
        {
            e1.printStackTrace();
        }
        
        // new buffered reader from the stream.
        final BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
        
        try
        {
            final Scanner scanner = new Scanner(new FileInputStream(tmpFile.getAbsolutePath()));
            // read from the csv file.
            while (scanner.hasNextLine())
            {
                final String csvLine = scanner.nextLine();
                
                final String[] csvElements = csvLine.split(",");
                
                // the first line of these csv files have the information of
                // what the values are. We dont need this.
                if ("Warning".equals(csvElements[0]))
                {
                    continue;
                }
                
                if (csvElements.length < 8)
                {
                    continue;
                }
                
                // get the weakness description
                String msg = csvElements[7];
                if (msg.startsWith("\""))
                {
                    msg = msg.substring(1, msg.length());
                }
                if (msg.endsWith("\""))
                {
                    msg = msg.substring(0, msg.length() - 1);
                }
                
                // get the weakness id.
                final String id = csvElements[2];
                // get the linenumber of the weakness.
                final Integer lineNumber = Integer.valueOf(csvElements[5]);
                // this tool has no offset.
                final Integer offset = null;
                // get the position of the weakness.
                final Integer position = Integer.valueOf(csvElements[6]);
                
                // This tool is funny in that it doesn't give you the full path
                // of the file. Therefore, we must construct the full path
                // ourselves.
                final int index = csvElements[4].lastIndexOf(File.separator) + 1;
                final String fileName = path + File.separator + csvElements[4].substring(index);
                
                // if there is a dataElement, use it.
                String dataElement = null;
                if (msg.split(":").length == 2)
                {
                    dataElement = msg.split(":")[1].trim();
                }
                // create the finding.
                creator.create(StringEscapeUtils.unescapeHtml4(msg), id, lineNumber, offset, position, file, dataElement, null);
            }
            
            // delete the file we made in the tmp directory.
            // tmpFile.delete();
        }
        catch (final FileNotFoundException e)
        {
            System.err.println("\nThe tools output file could not be found. " + tmpFile.getAbsolutePath() + "\n\n");
            System.exit(1);
        }
        
        // return the elements from the finding creator.
        return creator.getElements();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.kdmanalytics.toif.framework.toolAdaptor.AbstractAdaptor#
     * getAdaptorVendorAddress()
     */
    @Override
    public String getAdaptorVendorAddress()
    {
        return "3730 Richmond Rd, Suite 204, Ottawa, ON, K2H 5B9";
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.kdmanalytics.toif.framework.toolAdaptor.AbstractAdaptor#
     * getAdaptorVendorDescription()
     */
    @Override
    public String getAdaptorVendorDescription()
    {
        return "KDM Analytics is a security assurance company providing products and services for threat risk assessment and management, due diligence assessments, and information and data assurance. Leveraging our decades of experience in static analysis, reverse engineering and formal methods, we have created breakthrough products for the automated and systematic investigation of code, data and networks.";
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.kdmanalytics.toif.framework.toolAdaptor.AbstractAdaptor#
     * getAdaptorVendorEmail()
     */
    @Override
    public String getAdaptorVendorEmail()
    {
        return "adam@kdmanalytics.com";
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.kdmanalytics.toif.framework.toolAdaptor.AbstractAdaptor#
     * getAdaptorVendorName()
     */
    @Override
    public String getAdaptorVendorName()
    {
        return "KDM Analytics";
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.kdmanalytics.toif.framework.toolAdaptor.AbstractAdaptor#
     * getAdaptorVendorPhone()
     */
    @Override
    public String getAdaptorVendorPhone()
    {
        return "613-627-1011";
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.kdmanalytics.toif.framework.toolAdaptor.AbstractAdaptor#
     * getGeneratorDescription()
     */
    @Override
    public String getGeneratorDescription()
    {
        return "Splint is a tool for statically checking C programs for security vulnerabilities and coding mistakes.";
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * com.kdmanalytics.toif.framework.toolAdaptor.AbstractAdaptor#getGeneratorName
     * ()
     */
    @Override
    public String getGeneratorName()
    {
        return "splint";
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.kdmanalytics.toif.framework.toolAdaptor.AbstractAdaptor#
     * getGeneratorVendorAddress()
     */
    @Override
    public String getGeneratorVendorAddress()
    {
        return "http://www.splint.org";
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.kdmanalytics.toif.framework.toolAdaptor.AbstractAdaptor#
     * getGeneratorVendorDescription()
     */
    @Override
    public String getGeneratorVendorDescription()
    {
        return "University of Virginia Department of Computer Science";
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.kdmanalytics.toif.framework.toolAdaptor.AbstractAdaptor#
     * getGeneratorVendorEmail()
     */
    @Override
    public String getGeneratorVendorEmail()
    {
        return "info@splint.org";
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.kdmanalytics.toif.framework.toolAdaptor.AbstractAdaptor#
     * getGeneratorVendorName()
     */
    @Override
    public String getGeneratorVendorName()
    {
        return "David Evans";
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.kdmanalytics.toif.framework.toolAdaptor.AbstractAdaptor#
     * getGeneratorVendorPhone()
     */
    @Override
    public String getGeneratorVendorPhone()
    {
        return "";
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.kdmanalytics.toif.framework.toolAdaptor.AbstractAdaptor#
     * getGeneratorVersion()
     */
    @Override
    public String getGeneratorVersion()
    {
        final String[] commands = { "splint", "-help", "version" };
        ProcessBuilder splint = new ProcessBuilder(commands);
        try
        {
            Process splintInstance = splint.start();
            InputStream in = splintInstance.getErrorStream();
            
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            
            String strLine;
            
            while ((strLine = br.readLine()) != null)
            {
                String[] stringArray = strLine.split(" ");
                
                if (stringArray[1].trim().equals("3.1.2"))
                {
                    return stringArray[1].trim();
                }
                else
                {
                    System.err.println(getAdaptorName() + ": Generator " + stringArray[1] + " found, only version 3.1.2 has been tested");
                    return stringArray[1].trim();
                }
            }
            
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        
        return "";
    }
    
}