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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.resources.FileResource;

/**
 * @author <a href="http://community.jboss.org/people/dan.j.allen">Dan Allen</a>
 */
public class ServiceProviderDescriptor {
    private String serviceType;
    private Set<String> serviceProviders;
    private FileResource<?> configResource;
    
    public ServiceProviderDescriptor(final String serviceType, final ResourceFacet resources) throws IOException {
        this.serviceType = serviceType;
        load(resources);
    }
    
    public String getServiceType() {
        return serviceType;
    }
    
    public void setServiceProviders(final List<String> serviceProviders) {
        this.serviceProviders = new LinkedHashSet<String>(serviceProviders);
    }
    
    public Set<String> getServiceProviders() {
        return serviceProviders;
    }
    
    public boolean isProviderRegistered(String provider) {
        return serviceProviders.contains(provider);
    }
    
    public void save() {
        if (serviceProviders.size() > 0) {
            configResource.setContents(join(serviceProviders, "\n"));
        }
        else if (configResource.exists()) {
            configResource.delete();
        }
    }
    
    protected void load(final ResourceFacet resources) throws IOException {
        configResource = resources.getResource("META-INF/services/" + serviceType);
        if (configResource.exists()) {
            String content = readContents(configResource.getResourceInputStream());
            serviceProviders = new LinkedHashSet<String>(Arrays.asList(content.split("\n")));
        }
        else {
            serviceProviders = new LinkedHashSet<String>();
        }
    }
    
    private String readContents(final InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            int read;
            while ((read = is.read()) != -1) {
                out.write(read);
            }
        } finally {
            try {
                is.close();
            } catch (Exception e) {
            }
        }
        return out.toString();
    }
    
    private String join(final Collection<String> list, final String delimiter) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Iterator<String> it = list.iterator(); it.hasNext();) {
            if (first) {
                first = false;
            }
            else {
                builder.append(delimiter);
            }
            builder.append(it.next());
        }
        return builder.toString();
    }
}
