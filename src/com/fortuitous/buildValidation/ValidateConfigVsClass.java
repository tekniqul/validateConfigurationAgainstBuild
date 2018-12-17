/* Copyright 2018 Harold Fortuin of
   Fortuitous Consulting Services, Inc.

   You are free to use or modify this software and source code
   as long as you include this Copyright notice.

   No warranty is provided or implied. Use at your own risk.
*/
package com.fortuitous.buildValidation;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.configuration.HierarchicalConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

public class ValidateConfigVsClass {

	// to please Eclipse
	static final long serialVersionUID = 927394629374927L;

    static final String XPATH = "xPath";
    static final String IGNORE_PACKAGE = "ignorePackage";

    public static final char PATH_DELIMITER = '/'; // works for Windows too these days

    // log4j
    private static final Logger logr = Logger.getLogger(ValidateConfigVsClass.class);

    // holds each unique xPath in the classPaths.xml
    private static Collection<String> xPathPropCollection = null;

    // holds each unique ignorePackage in the classPaths.xml
    private static Collection<String> ignorePackageCollection = null;

    /** Usage: java ValidateConfigVsClass [settings]

     where settings should include each of these:
     [0] = classXPaths.xml - filetypes (filetype-xml) and associated xPath entries with
           fully qualified class names

     also includes packages (and therefore contained classes) that should be ignored;
     say for the Spring Dispatcher servlet, which won't be in your project's class files

     [1] = configurationFiles.xml - each config file has a unique configurationFile type attribute;
             each configurationFile element has subelements: location for the XML file,
                 classRootDirectory for the related class root directoryd
     */
    public static void main(String[] args) throws NullPointerException {

        // if any argument missing, the ArrayOutOfBoundsException is thrown
        String axPathConfigFile = args[0];
        String aConfigurationFilesXmlFile = args[1];

       logr.trace("args[0], the classXPaths.xml path/file = " + args[0]);
       logr.trace("args[1], the configurationFiles.xml path/file = " + args[1]);

        String xmlFileType = null;

        XMLConfiguration xmlXPathsConfig = null;
        XMLConfiguration xmlConfig = null;

        XPathExpressionEngine xPathEngine = null;

        File xPathsFile = null;
        File xmlConfigurationsFile = null;

        Object xPathProp = null;

        Object xPathConfigurationFiles = null;

        String xPathValue = null;
        // nice to have when debugging
        int countXPath = -1;

        Iterator<String> xPathsIterator = null;
        Iterator<HierarchicalConfiguration> configsIterator = null;

        String xmlClassRoot = null;
        String xmlFiletypeLocation = null;

        // nice to have when debugging
        boolean bClassFilesPerXPathExist = false;

        try {
        	// open and parse the XML that lists each file type and relevant directories
        	xmlConfigurationsFile = new File(aConfigurationFilesXmlFile);
        	// throws ClassFileException if file not found or lacking read permissions
        	boolean bIsXmlFileValid = existsAndIsFile(xmlConfigurationsFile, aConfigurationFilesXmlFile);

            xmlConfig = new XMLConfiguration(xmlConfigurationsFile);
            xPathEngine = new XPathExpressionEngine();

            xmlConfig.setExpressionEngine( xPathEngine );

            // reset for reuse later
            bIsXmlFileValid = false;

            logr.debug("xPathConfigurationFile=" + xPathConfigurationFiles);

            List<HierarchicalConfiguration> configurationFileElements =
            		xmlConfig.configurationsAt("configurationFile");
            HierarchicalConfiguration hconfConfigFile = null;

            configsIterator = configurationFileElements.iterator();

            while (configsIterator.hasNext()) {
            	hconfConfigFile = configsIterator.next();

        		// parse filetypes and iterate over each below
        		xmlFileType = hconfConfigFile.getString("@type");
        		logr.trace("xmlFileType = " + xmlFileType);

        		// root directory of .class files
        		xmlClassRoot = hconfConfigFile.getString("classRootDirectory");
        		logr.trace("xmlFiletypeClassRoot = " + xmlClassRoot);

        		// filetype's location
        		xmlFiletypeLocation = hconfConfigFile.getString("location");
        		logr.trace("xmlFiletypeLocation = " + xmlFiletypeLocation);

        		// now we have each fileLocationsConfig entry
        		xPathsFile = new File(axPathConfigFile);
        		// throws ClassFileException if file not found or lacking read permissions
        		bIsXmlFileValid = existsAndIsFile(xPathsFile, axPathConfigFile);

        		xmlXPathsConfig = new XMLConfiguration(xPathsFile);

        		/* I standardized the conversion of "web.xml" to "web-xml"
        		 * and similar for any other filetype, so we don't encounter XPath issues downstream as
        		 * in populateIgnorePackageCollection();
        		 *
        		 * Otherwise, replace the . with - in configurationFiles.xml, to match classXPaths.xml element names
        		   xmlFileType = xmlFileType.replace('.',  '-');
        		*/

        		ignorePackageCollection = populateIgnorePackageCollection(xmlXPathsConfig, xmlFileType);

        		xPathProp = xmlXPathsConfig.getProperty(xmlFileType + "." + XPATH);
        		// returns an ArrayList if > 1 xPath node
        		logr.trace("xPathProp class = " + xPathProp.getClass().getName() );

        		if (xPathProp instanceof Collection) {

        			xPathPropCollection = (Collection<String>) xPathProp;
        			countXPath = xPathPropCollection.size();
        			logr.trace("count of xPath nodes=" + countXPath);

        			xPathsIterator = xPathPropCollection.iterator();

        			while (xPathsIterator.hasNext()) {
        				xPathValue = xPathsIterator.next();

        				// process each xPath in file
        				bClassFilesPerXPathExist = classFilesPerXPathExist(xmlFileType, xmlFiletypeLocation, xPathValue, xmlClassRoot);
        			} // end while
        		} // end if Collection
        		// if only 1 xPath specified
        		else if (xPathProp instanceof String) {
        			countXPath = 1;
        			logr.trace("count of xPath nodes=" + countXPath);
        			xPathValue = (String)xPathProp;
        			// process the 1 xPath
        			bClassFilesPerXPathExist = classFilesPerXPathExist(xmlFileType, xmlFiletypeLocation, xPathValue, xmlClassRoot);
        		}
            } // end while for configurationFiles.xml
        } catch (Exception e) {
            e.printStackTrace();
        } // end catch

        logr.info("processing complete");
    } // end main()


    private static Collection<String> populateIgnorePackageCollection(XMLConfiguration xmlXPathsConfig, String aXmlFileType) {

        Collection<String> returnCollection = null;

        Object ignorePkgProp = null;
        int countXPath = -1;
        final String xpathProperty = aXmlFileType + "." + IGNORE_PACKAGE;

        try
        {
            logr.trace("xmlXPathsConfig object = " + xmlXPathsConfig);

            ignorePkgProp = xmlXPathsConfig.getProperty(xpathProperty);
           if (ignorePkgProp == null)
               throw new java.util.InvalidPropertiesFormatException("Property " + xpathProperty + " is invalid");

            logr.trace("ignorePkgProp class = " + ignorePkgProp.getClass().getName() );

            if (ignorePkgProp instanceof Collection) {
            	returnCollection = (Collection<String>)ignorePkgProp;
            	countXPath = returnCollection.size();
            } else if (ignorePkgProp instanceof String) {
            	String strIgnorePkgProp = (String)ignorePkgProp;
            	returnCollection = new ArrayList<String>();

            	if (!strIgnorePkgProp.equals("") ) {
            		returnCollection.add((String)ignorePkgProp);
            		countXPath = 1;
            	} else // the string IS empty
            		countXPath = 0;
            } // end String if

            logr.trace("count of ignorePkgProp nodes=" + countXPath);
        } catch (Exception e) {
            e.printStackTrace();
        } // end catch

        return returnCollection;
    } // end populateIgnorePackageCollection(..)

    /** for each unique xPath,
     check that EACH of n matching entries in the (web.xml) has a corresponding .class

     presuming for now a XML fileLocation
     */
    private static boolean classFilesPerXPathExist(String xmlFileType, String xmlFileLocation, String xPath, String classRoot)
        throws ClassFileException, Exception {
        boolean exists = false;

       logr.info("classFilesPerXPathExist() arg: xmlFileType=" + xmlFileType);
        // xpath printed later

        File xmlConfigFile = null;
        XMLConfiguration xmlConfig = null;
        String xPathValue = null;

        Object xPathProp = null;

        int countXPath = -1;

        // @SuppressWarnings("unchecked")
        Collection<String> xPathInstanceCollection = null;

        Iterator<String> xPathsIterator = null;

        boolean bClassFileInstanceExists = true;

        try
        {

            // get the n matching xPath's in the environment xmlFileLocation
            xmlConfigFile = new File(xmlFileLocation);

            // TEST if exists AT ALL -
            logr.trace("*** xmlConfigFile = " + xmlConfigFile.toString() );
            existsAndIsFile(xmlConfigFile, xmlFileLocation);

            xmlConfig = new XMLConfiguration(xmlConfigFile);
            XPathExpressionEngine xPathEngine = new XPathExpressionEngine();

            xmlConfig.setExpressionEngine( xPathEngine );

            xPathProp = xmlConfig.getList(xPath);

            logr.debug("xPathProp=" + xPathProp);

            // returns an ArrayList if > 1 xPath node
            logr.debug("xPathProp class = " + xPathProp.getClass().getName() );

            if (xPathProp instanceof Collection) {

                xPathInstanceCollection = (Collection<String>)xPathProp;
                countXPath = xPathInstanceCollection.size();
               logr.trace("** ENTERING PER xPath-instance processing loop ***");
                xPathsIterator = xPathInstanceCollection.iterator();

                /* bClassFileInstanceExists initialized true
                   the loop quits on the first failure to confirm (existence && is-a-file-and-not-directory) of a  file
                   per File Javadoc, the OS settings must permit reading the file */
                while( xPathsIterator.hasNext() && bClassFileInstanceExists) {
                    xPathValue = xPathsIterator.next();
                    logr.trace("xPathValue=" + xPathValue);

                    // process each fully qualified class name
                    bClassFileInstanceExists = classFileInstanceExists(xPathValue, classRoot);
                } // end while

            } // end if Collection

      } catch (Exception e) {
            e.printStackTrace();
        }

        return exists;
    } // end classFilesPerXPathExist()


    /**	check for each  file instance;
     but if within an ignorePackage, still returns true
     */
    private static boolean classFileInstanceExists(String Path, String classRoot) {
        boolean bClassFileInstanceExists = false;

        // change delimiter from Java's . to PATH_DELIMITER (/)
        String osSourcePath = Path.replace('.', PATH_DELIMITER );

        // combine paths - works; we presume we have Java
        String totalPathToFile = classRoot + PATH_DELIMITER + osSourcePath + ".class";
        logr.debug("totalPathToFile =" + totalPathToFile);

        File theFile = null;

        try
        {
            // check if ignore-able - uses ORIGINAL Path for comparison
            if (!isInIgnorePackage(Path) ) {

                // confirm the totalPathToFile file exists and is a file
                theFile = new File(totalPathToFile);
                // create info & error strings, and write method

                logr.trace("*** CLASS FILE PROCESSING ***");
               bClassFileInstanceExists = existsAndIsFile(theFile, totalPathToFile);

             } else {
                logr.info("skip processing : " + Path);

                // since this file is within an ignorePackage, it's OK to bypass and continue processing
                bClassFileInstanceExists = true;
            }

        } catch (ClassFileException msfe) {
           logr.error(msfe);
            msfe.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bClassFileInstanceExists;
    } // end classFileInstanceExists()

    /** decides whether to bypass processing for this fully qualified class name,
     * based on containing package
     */
    private static boolean isInIgnorePackage(String Path) {
        // ignorePackageCollection has the relevant Strings
        boolean bIsInIgnorePackage = false;
        String ignorePkgValue = null;

        Iterator<String> ignorePkgIterator = ignorePackageCollection.iterator();

        try
        {
            ignorePkgIterator = ignorePackageCollection.iterator();

            // the loop quits on match
            while( ignorePkgIterator.hasNext() && !bIsInIgnorePackage) {
                ignorePkgValue = ignorePkgIterator.next();
                logr.trace("ignorePkgValue=" + ignorePkgValue);

                bIsInIgnorePackage = Path.startsWith(ignorePkgValue);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

        return bIsInIgnorePackage;
    } // end isInIgnorePackage(...)

private static boolean existsAndIsFile(File aFile, String aPathFile)
            throws ClassFileException {
    boolean bExistsAndIsFile = false;

    if (aFile.exists() ) {
        if (aFile.isFile() ) {
            bExistsAndIsFile = true;
            logr.info("** FILE: " + aPathFile + "\n" + " EXISTS **");
        }
        else
            throw new ClassFileException("Item: " + aPathFile +
                    "\n" + " is not a file. Perhaps a directory?");
    }
    else
        throw new ClassFileException("FILE: " + aPathFile +
                "\n" + " is either missing or lacks read permissions.");

    return bExistsAndIsFile;
    }

} // end class