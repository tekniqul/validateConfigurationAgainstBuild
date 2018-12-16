# validateConfigurationAgainstBuild
Check that (Java class) files referenced in (XML) configuration files exist in the build directory tree

The motivations behind the tool are explained best at
https://bit.ly/2zYGL1M

Remember that the failure to find one or more build files (.class files) could be from:
--incorrect configuration of this tool
--incorrect spelling of class or package name(s)
--incorrect spelling of the build directory tree root
--missing build file(s)
--in some operating systems, read-only status of build file(s)

and possibly other causes.

In any case, by identifying improper or missing configuration or build files, build personnel can prevent related runtime errors.

In this version, the tool only parses XML configuration files and expects .class files as the build files.
