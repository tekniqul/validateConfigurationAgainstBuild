REM  Note expected command-line arguments, each of which names a file (in the current directory), by zero-based index:
REM	[0] = classXPaths.xml - filetypes [filetype]-xml and associated xPath entries with
REM           fully qualified class names
REM    	can includes packages (and therefore contained classes) that should be ignored;
REM     say for the Spring Dispatcher servlet, which won't be in your project's class files
REM
REM     [1] = configurationFiles.xml - each config file has a unique configurationFile type attribute;
REM           each configurationFile element has subelements: location for the XML file, 
REM              classRootDirectory for the related class root directoryd
REM
REM	Also note the use of log4j, and thus its own log4j.xml.
REM	Note that command line output, per the log4j configuration, is also routed to a log.html for easier review.
REM
REM	Review the example files provided to help you setup your own validation
REM
REM	Should work with any recent version of Java, and this .bat should work with Windows 8 and above.
REM
java -jar ValidateConfigByClass.jar classXPaths.xml BAD_configurationFiles.xml