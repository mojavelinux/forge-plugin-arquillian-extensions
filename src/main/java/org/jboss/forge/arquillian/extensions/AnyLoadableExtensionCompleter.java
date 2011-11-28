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
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.jboss.forge.parser.java.InterfaceCapable;
import org.jboss.forge.parser.java.JavaSource;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.resources.DirectoryResource;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.resources.ResourceFilter;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.completer.SimpleTokenCompleter;

/**
 * @author <a href="http://community.jboss.org/people/dan.j.allen">Dan Allen</a>
 */
// TODO share abstract class w/ ClientLoadableExtensionCompleter
public class AnyLoadableExtensionCompleter extends SimpleTokenCompleter {

    @Inject
    private Project project;
    
    @Override
    public Iterable<JavaResource> getCompletionTokens() {
        JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
        return getLoadableExtensionSources(java.getSourceFolder());
    }

    public Set<JavaResource> getLoadableExtensionSources(final DirectoryResource dir) {
        Set<JavaResource> results = new HashSet<JavaResource>();
        for (Resource<?> resource : dir.listResources(new ResourceFilter() {
            @Override
            public boolean accept(Resource<?> r) {
                return r instanceof JavaResource || r instanceof DirectoryResource;
            }
        })) {
            if (resource instanceof DirectoryResource) {
                results.addAll(getLoadableExtensionSources((DirectoryResource) resource));
            }
            else if (resource instanceof JavaResource) {
                try {
                    JavaSource<?> source = ((JavaResource) resource).getJavaSource();
                    if (source instanceof InterfaceCapable &&
                            (((InterfaceCapable<?>) source).hasInterface(ArquillianExtensionsPlugin.LOADABLE_EXTENSION_TYPE) ||
                            ((InterfaceCapable<?>) source).hasInterface(ArquillianExtensionsPlugin.REMOTE_LOADABLE_EXTENSION_TYPE))) {
                        results.add((JavaResource) resource);
                    }
                } catch (FileNotFoundException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return results;
    }
}
