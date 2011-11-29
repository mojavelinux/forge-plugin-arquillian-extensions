/*
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.forge.arquillian.extensions;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.InterfaceCapable;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaSource;
import org.jboss.forge.parser.java.Method;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.PromptType;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellColor;
import org.jboss.forge.shell.events.PickupResource;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.Help;
import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.shell.plugins.SetupCommand;
import org.jboss.forge.shell.plugins.Topic;

/**
 * @author <a href="http://community.jboss.org/people/dan.j.allen">Dan Allen</a>
 */
@Topic("Arquillian")
@Alias("arquillian-extensions")
@RequiresFacet({JavaSourceFacet.class, ArquillianSPIFacet.class})
@Help("A plugin that helps creating Arquillian extensions")
public class ArquillianExtensionsPlugin implements Plugin {

    public static final String LOADABLE_EXTENSION_TYPE = "org.jboss.arquillian.core.spi.LoadableExtension";
    public static final String REMOTE_LOADABLE_EXTENSION_TYPE = "org.jboss.arquillian.container.test.spi.RemoteLoadableExtension";
    public static final String TEST_ENRICHER_TYPE = "org.jboss.arquillian.test.spi.TestEnricher";
    public static final String OBSERVES_TYPE = "org.jboss.arquillian.core.api.annotation.Observes";
    public static final String AUXILIARY_ARCHIVE_APPENDER_TYPE = "org.jboss.arquillian.container.test.spi.client.deployment.AuxiliaryArchiveAppender";
    public static final String ARCHIVE_TYPE = "org.jboss.shrinkwrap.api.Archive";
    public static final String SHRINKWRAP_TYPE = "org.jboss.shrinkwrap.api.ShrinkWrap";
    public static final String JAVA_ARCHIVE_TYPE = "org.jboss.shrinkwrap.api.spec.JavaArchive";
    
    public static final String LOADABLE_EXTENSION = toSimpleName(LOADABLE_EXTENSION_TYPE);
    public static final String REMOTE_LOADABLE_EXTENSION = toSimpleName(REMOTE_LOADABLE_EXTENSION_TYPE);
    public static final String EXTENSION_BUILDER = "ExtensionBuilder";
    public static final String TEST_ENRICHER = toSimpleName(TEST_ENRICHER_TYPE);
    public static final String OBSERVES = toSimpleName(OBSERVES_TYPE);
    public static final String AUXILIARY_ARCHIVE_APPENDER = toSimpleName(AUXILIARY_ARCHIVE_APPENDER_TYPE);
    public static final String ARCHIVE = toSimpleName(ARCHIVE_TYPE);
    public static final String SHRINKWRAP = toSimpleName(SHRINKWRAP_TYPE);
    public static final String JAVA_ARCHIVE = toSimpleName(JAVA_ARCHIVE_TYPE);

    private static final String REGISTER_METHOD_NAME = "register";
    private static final String RESOLVE_METHOD_NAME = "resolve";
    private static final String ENRICH_METHOD_NAME = "enrich";
    
    public static final String SERVICE_PROVIDER_TYPE = LOADABLE_EXTENSION_TYPE;
    public static final String SERVICE_PROVIDER_RESOURCE = "META-INF/services/" + SERVICE_PROVIDER_TYPE;
    
    @Inject
    private Project project;
    
    @Inject
    private Shell shell;
    
    @Inject
    private Event<InstallFacets> request;
    
    @Inject
    private Event<PickupResource> pickup;
    
    @Inject
    private ClientLoadableExtensionCompleter loadableExtensionCompleter;

    @SetupCommand(help = "Prepares the project for developing Arquillian extensions.")
    public void setup() {
        if (!project.hasFacet(ArquillianSPIFacet.class)) {
            request.fire(new InstallFacets(ArquillianSPIFacet.class));
        }
    }
    
    @Command(value = "new-extension", help = "Generates a new LoadableExtension class from the specified name and package.")
    public void newExtension(
            @Option(required = true, name = "named") final String extensionName,
            @Option(required = false, name = "package", type = PromptType.JAVA_PACKAGE) final String extensionPackage,
            @Option(required = false, name = "type", defaultValue = "client", completer = LoadableExtensionTypeCompleter.class) final String type)
                    throws FileNotFoundException {
        JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
        JavaClass extensionClass = JavaParser.create(JavaClass.class);
        extensionClass.setName(extensionName);
        extensionClass.setPackage(selectPackage("extension", extensionPackage, java.getBasePackage()));
        JavaResource extensionSource = java.getJavaResource(extensionClass);
        if (!extensionSource.exists()) {
            if (extensionSource.createNewFile()) {
                extensionClass.addImport(LOADABLE_EXTENSION_TYPE);
                extensionClass.addInterface(LOADABLE_EXTENSION);
                Method<JavaClass> registerMethod = extensionClass.addMethod()
                        .setPublic()
                        .setReturnTypeVoid()
                        .setName(REGISTER_METHOD_NAME);
                registerMethod.addAnnotation(Override.class);
                registerMethod.setParameters(EXTENSION_BUILDER + " builder");
                registerMethod.setBody("/** register extension implementations here **/");
                
                if ("remote".equals(type)) {
                    JavaClass remoteExtensionClass = JavaParser.create(JavaClass.class);
                    remoteExtensionClass.setName("Remote" + extensionName);
                    remoteExtensionClass.setPackage(extensionClass.getPackage());
                    JavaResource remoteExtensionSource = java.getJavaResource(remoteExtensionClass);
                    // TODO handle failure scenario
                    if (!remoteExtensionSource.exists() && remoteExtensionSource.createNewFile()) {
                        remoteExtensionClass.addImport(REMOTE_LOADABLE_EXTENSION_TYPE);
                        remoteExtensionClass.addInterface(REMOTE_LOADABLE_EXTENSION);
                        Method<JavaClass> remoteRegisterMethod = remoteExtensionClass.addMethod()
                                .setPublic()
                                .setReturnTypeVoid()
                                .setName(REGISTER_METHOD_NAME);
                        remoteRegisterMethod.addAnnotation(Override.class);
                        remoteRegisterMethod.setParameters(EXTENSION_BUILDER + " builder");
                        remoteRegisterMethod.setBody("");
                        remoteExtensionSource.setContents(remoteExtensionClass);
                        
                        JavaClass remoteExtensionAppenderClass = JavaParser.create(JavaClass.class);
                        remoteExtensionAppenderClass.setName(extensionName + "ArchiveAppender");
                        remoteExtensionAppenderClass.setPackage(extensionClass.getPackage());
                        JavaResource remoteExtensionAppenderSource = java.getJavaResource(remoteExtensionAppenderClass);
                        // FIXME leap of faith
                        remoteExtensionAppenderSource.createNewFile();
                        
                        remoteExtensionAppenderClass.addImport(AUXILIARY_ARCHIVE_APPENDER_TYPE);
                        remoteExtensionAppenderClass.addImport(REMOTE_LOADABLE_EXTENSION_TYPE);
                        remoteExtensionAppenderClass.addImport(SHRINKWRAP_TYPE);
                        remoteExtensionAppenderClass.addImport(ARCHIVE_TYPE);
                        remoteExtensionAppenderClass.addImport(JAVA_ARCHIVE_TYPE);
                        //remoteExtensionAppender.addImport(remoteExtensionClass.getQualifiedName());
                        remoteExtensionAppenderClass.addInterface(AUXILIARY_ARCHIVE_APPENDER);
                        Method<JavaClass> createArchiveMethod = remoteExtensionAppenderClass.addMethod()
                                .setPublic()
                                .setReturnType(ARCHIVE + "<?>")
                                .setName("createAuxiliaryArchive");
                        createArchiveMethod.addAnnotation(Override.class);
                        String archiveName = "arquillian-extension-" + extensionName.toLowerCase().replace("extension", "") + ".jar";
                        createArchiveMethod.setBody(
                                "return ShrinkWrap.create(JavaArchive.class, \"" + archiveName + "\")\n" +
                                "    .addClass(" + toClassRef(remoteExtensionClass.getName()) + ")\n" +
                                "    .addAsServiceProvider(" + toClassRef(REMOTE_LOADABLE_EXTENSION) + ", " + toClassRef(remoteExtensionClass.getName()) + ");");
                        
                        remoteExtensionAppenderSource.setContents(remoteExtensionAppenderClass);
                        
                        extensionClass.addImport(AUXILIARY_ARCHIVE_APPENDER_TYPE);
                        registerMethod.setBody("builder.service(" + toClassRef(AUXILIARY_ARCHIVE_APPENDER) + ", " + toClassRef(remoteExtensionAppenderClass.getName()) + ");");
                    }
                }
                else {
                }
                
                extensionSource.setContents(extensionClass);
                shell.println("Created loadable extension [" + extensionClass.getQualifiedName() + "]");
    
                registerExtension(extensionSource);
    
                //pickup.fire(new PickupResource(extensionSource));
            }
            else {
                shell.println(ShellColor.RED, "Could not write Java source file [" + extensionClass.getQualifiedName() + "]");
            }
        }
        else {
            shell.println(ShellColor.YELLOW, "Java source file already exists [" + extensionClass.getQualifiedName() + "]");
        }
    }
    
    @Command(value = "new-test-enricher",
            help = "Generates a new TestEnricher class from the specified name and package and, if an extension is specified, registers the new type in the extension.")
    public void newTestEnricher(
            @Option(required = true, name = "named") final String enricherName,
            @Option(required = false, name = "package") final String enricherPackage,
            @Option(required = false, name = "forExtension", completer = AnyLoadableExtensionCompleter.class) final JavaResource extensionType
            ) throws FileNotFoundException {
        JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
        JavaClass javaClass = JavaParser.create(JavaClass.class);
        javaClass.setName(enricherName);
        javaClass.setPackage(selectPackage("enricher", enricherPackage, java.getBasePackage()));
        JavaResource javaResource = java.getJavaResource(javaClass);
        
        if (!javaResource.exists()) {
            if (javaResource.createNewFile()) {
                javaClass.addImport(TEST_ENRICHER_TYPE);
                javaClass.addInterface(TEST_ENRICHER);
                Method<JavaClass> enrichMethod = javaClass.addMethod()
                        .setPublic()
                        .setReturnTypeVoid()
                        .setName(ENRICH_METHOD_NAME);
                enrichMethod.addAnnotation(Override.class);
                enrichMethod.setParameters("Object testCase");
                // FIXME comment is being dropped
                enrichMethod.setBody("/** enrich test case object here **/");
                
                javaClass.addImport(java.lang.reflect.Method.class);
                Method<JavaClass> resolveMethod = javaClass.addMethod()
                        .setPublic()
                        .setReturnType("Object[]")
                        .setName(RESOLVE_METHOD_NAME);
                resolveMethod.addAnnotation(Override.class);
                resolveMethod.setParameters("Method method");
                resolveMethod.setBody("return new Object[method.getParameterTypes().length];");
                
                javaResource.setContents(javaClass);
                pickup.fire(new PickupResource(javaResource));
                
                // TODO print warning if doesn't exist or implement loadable extension
                if (extensionType.exists() &&
                        (isLoadableExtension(extensionType.getJavaSource()) ||
                        isRemoteLoadableExtension(extensionType.getJavaSource()))) {
                    JavaClass extensionClass = (JavaClass) extensionType.getJavaSource();
                    extensionClass.addImport(TEST_ENRICHER_TYPE);
                    if (!javaClass.getPackage().equals(extensionClass.getPackage())) {
                        extensionClass.addImport(javaClass.getQualifiedName());
                    }
                    Method<JavaClass> registerMethod = extensionClass.getMethod(REGISTER_METHOD_NAME, EXTENSION_BUILDER);
                    String builderName = registerMethod.getParameters().get(0).getName();
                    registerMethod.setBody(registerMethod.getBody() + "    " +
                            builderName + ".service(" + toClassRef(TEST_ENRICHER) + ", " + toClassRef(javaClass.getName()) + ");\n    ");
                    extensionType.setContents(extensionClass);
                }
            }
        }
        else {
            shell.println(ShellColor.YELLOW, "Java source file already exists [" + javaClass.getQualifiedName() + "]");
        }
    }
    
    // TODO add additional parameters to observer based on event type
    @Command(value = "new-observer")
    public void newObserver(
            @Option(required = true, name = "named") final String observerName,
            @Option(required = false, name = "package") final String observerPackage,
            @Option(required = true, name = "eventType", completer = ArquillianEventsCompleter.class) final String eventType,
            @Option(required = false, name = "forExtension", completer = AnyLoadableExtensionCompleter.class) final JavaResource extensionType)
            throws FileNotFoundException {
        JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
        JavaClass javaClass = JavaParser.create(JavaClass.class);
        javaClass.setName(observerName);
        javaClass.setPackage(selectPackage("observer", observerPackage, java.getBasePackage()));
        JavaResource javaResource = java.getJavaResource(javaClass);
        if (!javaResource.exists()) {
            if (javaResource.createNewFile()) {
                String eventName = toSimpleName(eventType);
                javaClass.addImport(OBSERVES_TYPE);
                javaClass.addImport(eventType);
                Method<JavaClass> observerMethod = javaClass.addMethod()
                    .setPublic()
                    .setReturnTypeVoid()
                    .setName("execute" + eventName);
                observerMethod.setParameters("@Observes " + eventName + " event");
                observerMethod.setBody("");
                
                javaResource.setContents(javaClass);
                pickup.fire(new PickupResource(javaResource));
                // TODO print warning if doesn't exist or implement loadable extension
                if (extensionType.exists() &&
                        (isLoadableExtension(extensionType.getJavaSource()) ||
                        isRemoteLoadableExtension(extensionType.getJavaSource()))) {
                    JavaClass extensionClass = (JavaClass) extensionType.getJavaSource();
                    if (!javaClass.getPackage().equals(extensionClass.getPackage())) {
                        extensionClass.addImport(javaClass.getQualifiedName());
                    }
                    Method<JavaClass> registerMethod = extensionClass.getMethod(REGISTER_METHOD_NAME, EXTENSION_BUILDER);
                    String builderName = registerMethod.getParameters().get(0).getName();
                    registerMethod.setBody(registerMethod.getBody() + "    " +
                            builderName + ".observer(" + toClassRef(javaClass.getName()) + ");\n    ");
                    extensionType.setContents(extensionClass);
                }
            }
        }
        else {
            shell.println(ShellColor.YELLOW, "Java source file already exists [" + javaClass.getQualifiedName() + "]");
        }
    }
    
    @Command(value = "list-registered", help = "Lists the LoadableExtension classes registered as service providers.")
    public void listRegisteredExtensions() {
        try {
            for (String provider : getServiceProviderDescriptor().getServiceProviders()) {
                shell.println(provider);
            }
        }
        catch (IOException e) {
            shell.println(ShellColor.RED, "Could not read service provider configuration resource [" + SERVICE_PROVIDER_RESOURCE + "].");
        }
    }
    
    @Command(value = "register-all", help = "Registers all LoadableExtension classes found in project as service providers")
    public void registerExtensions() {
        JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
        for (JavaResource e : loadableExtensionCompleter.getLoadableExtensionSources(java.getSourceFolder())) {
            try {
                registerExtension(e);
            } catch (FileNotFoundException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
    
    @Command(value = "register",
            help = "Registers an existing LoadableExtension class as a service provider.")
    public void registerExtension(
            @Option(required = true, completer = ClientLoadableExtensionCompleter.class) final JavaResource provider)
                    throws FileNotFoundException {
        JavaSource<?> providerSource = provider.getJavaSource();
        String providerType = providerSource.getQualifiedName();
        if (isLoadableExtension(providerSource)) {
            try {
                ServiceProviderDescriptor providerResource = getServiceProviderDescriptor();
                providerResource.getServiceProviders().add(providerType);
                providerResource.save();
            }
            catch (IOException e) {
                shell.println(ShellColor.RED, "Failed to register the specified extension [" + providerType + "]");
            }
            shell.println("Registered specified extension as service provider [" + providerType + "]");
        }
        else {
            shell.println(ShellColor.RED, "Specified type is not a LoadableExtension [" + providerType + "]");
        }
    }
    
    @Command(value = "unregister-all", help = "Unregisters all LoadableExtension classes as service providers")
    public void unregisterExtensions() {
        try {
            ServiceProviderDescriptor providerResource = getServiceProviderDescriptor();
            providerResource.getServiceProviders().clear();
            providerResource.save();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
    
    @Command(value = "unregister",
            help = "Unregisters an existing LoadableExtension class as a service provider.")
    public void unregisterExtension(
            @Option(required = true, completer = ClientLoadableExtensionCompleter.class) final JavaResource provider)
                    throws FileNotFoundException {
        JavaSource<?> providerSource = provider.getJavaSource();
        String providerType = providerSource.getQualifiedName();
        if (isLoadableExtension(providerSource)) {
            try {
                ServiceProviderDescriptor providerResource = getServiceProviderDescriptor();
                providerResource.getServiceProviders().remove(providerType);
                providerResource.save();
            }
            catch (IOException e) {
                shell.println(ShellColor.RED, "Failed to unregister the specified extension [" + providerType + "]");
            }
            shell.println("Unregistered specified extension as service provider [" + providerType + "]");
        }
        else {
            shell.println(ShellColor.RED, "Specified type is not a LoadableExtension [" + providerType + "]");
        }
    }
    
    // TODO add type option filter
    @Command(value = "list", help = "Lists each LoadableExtension and/or RemoteLoadableExtension classes in this project.")
    public void list() {
        JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
        for (JavaResource ext : new AnyLoadableExtensionCompleter().getLoadableExtensionSources(java.getSourceFolder())) {
            try {
                JavaSource<?> source = ext.getJavaSource();
                String type = ((InterfaceCapable<?>) source).hasInterface(LOADABLE_EXTENSION_TYPE) ?
                        LOADABLE_EXTENSION_TYPE : REMOTE_LOADABLE_EXTENSION_TYPE;
                shell.println(source.getQualifiedName() + " [" + type + "]");
            } catch (FileNotFoundException e) {}
        }
    }
    
    protected String selectPackage(String typeName, String specifiedPackage, String basePackage) {
        String pkg = null;
        if (specifiedPackage != null && specifiedPackage.length() > 0) {
            pkg = specifiedPackage;
        }
        else {
            pkg = shell.promptCommon(
                    "In which package you'd like to create this " + typeName + " " +
                    "(press ENTER to accept default [" + basePackage + "]):",
                    PromptType.JAVA_PACKAGE, basePackage);
        }
        
        return pkg;
    }
    
    private ServiceProviderDescriptor getServiceProviderDescriptor() throws IOException
    {
        return new ServiceProviderDescriptor(
                SERVICE_PROVIDER_TYPE, project.getFacet(ResourceFacet.class));
    }
    
    private boolean isLoadableExtension(JavaSource<?> source) {
        return source instanceof InterfaceCapable &&
                ((InterfaceCapable<?>) source).hasInterface(LOADABLE_EXTENSION_TYPE);
    }
    
    private boolean isRemoteLoadableExtension(JavaSource<?> source) {
        return source instanceof InterfaceCapable &&
                ((InterfaceCapable<?>) source).hasInterface(REMOTE_LOADABLE_EXTENSION_TYPE);
    }
    
    private static String toSimpleName(final String fqn) {
        return fqn.substring(fqn.lastIndexOf('.') + 1);
    }
    
    private static String toClassRef(final String className) {
        return className + ".class";
    }
}