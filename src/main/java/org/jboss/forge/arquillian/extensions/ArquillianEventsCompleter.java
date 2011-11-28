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

import java.util.Arrays;

import org.jboss.forge.shell.completer.SimpleTokenCompleter;

/**
 * @author <a href="http://community.jboss.org/people/dan.j.allen">Dan Allen</a>
 */
public class ArquillianEventsCompleter extends SimpleTokenCompleter {

    // TODO fill in remaining event options
    public static final String[] events = {
        "org.jboss.arquillian.test.spi.event.suite.After",
        "org.jboss.arquillian.test.spi.event.suite.AfterClass",
        "org.jboss.arquillian.test.spi.event.suite.AfterSuite",
        "org.jboss.arquillian.test.spi.event.suite.Before",
        "org.jboss.arquillian.test.spi.event.suite.BeforeClass",
        "org.jboss.arquillian.test.spi.event.suite.BeforeSuite",
        "org.jboss.arquillian.test.spi.event.suite.Test",
        "org.jboss.arquillian.test.spi.event.suite.TestEvent",
        "org.jboss.arquillian.container.spi.event.container.AfterDeploy",
        "org.jboss.arquillian.container.spi.event.container.AfterSetup",
        "org.jboss.arquillian.container.spi.event.container.AfterStart",
        "org.jboss.arquillian.container.spi.event.container.AfterStop",
        "org.jboss.arquillian.container.spi.event.container.AfterUnDeploy",
        "org.jboss.arquillian.container.spi.event.container.BeforeDeploy",
        "org.jboss.arquillian.container.spi.event.container.BeforeSetup",
        "org.jboss.arquillian.container.spi.event.container.BeforeStart",
        "org.jboss.arquillian.container.spi.event.container.BeforeStop",
        "org.jboss.arquillian.container.spi.event.container.BeforeUnDeploy",
    };
    
    @Override
    public Iterable<String> getCompletionTokens() {
        return Arrays.asList(events);
    }
}
