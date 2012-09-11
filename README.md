# Arquillian Extensions Plugin for Forge

This plugin is intended to help you convert a basic Java project into an Arquillian Extensions project. It also assists you with creating and managing extension classes.

## Installation

Currently, the plugin is in a prereleased state. You can build it and install it into Forge from this github repository.

First, start Forge.

    $> forge

Then, use the forge git-source plugin to download, build and install the plugin into your Forge shell.

    $> forge git-plugin git://github.com/mojavelinux/forge-plugin-arquillian-extensions.git

When the build is complete, the new arquillian-extensions command will be available in any Java project.

## Setting up the Arquillian SPIs

To prepare the project for writing Arquillian Extensions, you first need to add the dependencies to your project. This is accomplished by using the arquillian-extensions setup command from Forge:

    $> setup arquillian-extensions

This command simply adds the Arquillian BOM and all the Arquillian SPI artifacts.

## Creating a new client-side (loadable) extension

The first step in writing an Arquillian extension is to create a LoadableExtension class where the extension classes get registered. A LoadableExtension is an Arquillian-specific abstraction over the Java ServiceLoader.

    $> arquillian-extensions new-extension --named MyExtension

This command will create a (client) LoadableExtension class in the default directory (unless you specified a package) and register it in the META-INF/services/org.jboss.arquillian.core.spi.LoadableExtension descriptor.

When creating extension classes, you can now reference your LoadableExtension class and the plugin will automatically register the extension the appropriate way.

## Creating a new remote (loadable) extension

If the extension is supposed to run on the server (in-container), you need to instead create a RemoteLoadableExtension. This can be accomplished by using the type option when creating the extension.

    $> arquillian-extensions new-extension --named MyInContainerExtension --type remote

This command will create a RemoteLoadableExtension class in the default directory (unless you specified a package), create an auxiliary archive appender and register the extension in the META-INF/services/org.jboss.arquillian.container.test.spi.RemoteLoadableExtension descriptor in that auxiliary archive.

## Creating a new test enricher

Use the following command to create a new test enricher. In this case, we'll create it for our RemoteLoadableExtension.

    $> arquillian-extensions new-test-enricher --named AlienProbeTestEnricher --forExtension org.example.MyInContainerExtension

This command will create a TestEnricher class in the default directory (unless you specified a package) and register it using the ExtensionBuilder SPI in the register method of the RemoteLoadableExtension.

## More to come...
