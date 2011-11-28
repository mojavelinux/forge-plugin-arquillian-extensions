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

import java.util.List;

import javax.inject.Inject;

import org.jboss.forge.project.dependencies.Dependency;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.dependencies.ScopeType;
import org.jboss.forge.project.facets.BaseFacet;
import org.jboss.forge.project.facets.DependencyFacet;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.packaging.PackagingType;
import org.jboss.forge.shell.ShellPrompt;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.shell.plugins.RequiresPackagingType;

/**
 * @author <a href="http://community.jboss.org/people/dan.j.allen">Dan Allen</a>
 */
@Alias("forge.arquillian.spi")
@RequiresFacet(JavaSourceFacet.class)
@RequiresPackagingType(PackagingType.JAR)
public class ArquillianSPIFacet extends BaseFacet {

    private static final String[] dependencies = {
        "org.jboss.arquillian.core:arquillian-core-spi",
        "org.jboss.arquillian.test:arquillian-test-spi",
        "org.jboss.arquillian.container:arquillian-container-spi",
        "org.jboss.arquillian.container:arquillian-container-test-spi",
        "org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-api-maven"
    };
    
    @Inject
    private ShellPrompt prompt;
    
    @Override
    public boolean install() {
        if (isInstalled()) {
            return true;
        }
        
        DependencyFacet projectDeps = project.getFacet(DependencyFacet.class);
        DependencyBuilder dep = null;
        //String arquillianVersion = null;
        
        dep = DependencyBuilder.create("org.jboss.arquillian:arquillian-bom")
                .setScopeType(ScopeType.IMPORT)
                .setPackagingType("pom");
        if (!projectDeps.hasManagedDependency(dep)) {
            List<Dependency> dependencies = projectDeps.resolveAvailableVersions(dep);
            Dependency resolvedDep = prompt.promptChoiceTyped("Which version of Arquillian Core do you want to target?", dependencies);
            //arquillianVersion = resolvedDep.getVersion();
            projectDeps.addManagedDependency(resolvedDep);
        }
        else {
            //arquillianVersion = projectDeps.getManagedDependency(dep).getVersion();
        }
        addSPIDependencies(projectDeps);
        return true;
    }

    @Override
    public boolean isInstalled()
    {
       DependencyFacet projectDeps = project.getFacet(DependencyFacet.class);
       for (String d : dependencies) {
           if (!projectDeps.hasDependency(DependencyBuilder.create(d))) {
               return false;
           }
       }
       return true;
    }
    
    private void addSPIDependencies(DependencyFacet projectDeps) {
        for (String d : dependencies) {
            DependencyBuilder dep = DependencyBuilder.create(d);
            if (!projectDeps.hasDependency(dep)) {
                projectDeps.addDependency(dep);
            }
        }
    }
}
