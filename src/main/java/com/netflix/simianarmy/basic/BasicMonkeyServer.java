/*
 *
 *  Copyright 2012 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.simianarmy.basic;

import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.MonkeyRunner;
import com.netflix.simianarmy.basic.chaos.BasicChaosMonkey;

/**
 * The Class BasicMonkeyServer.
 */
@SuppressWarnings("serial")
public class BasicMonkeyServer extends HttpServlet {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicMonkeyServer.class);

    /** The Constant RUNNER. */
    private static final MonkeyRunner RUNNER = MonkeyRunner.getInstance();

    /**
     * Add monkeys to run.
     */
    public void addMonkeys() {
        RUNNER.replaceMonkey(BasicChaosMonkey.class, BasicContext.class);
    }

    /**
     * Inits the server.
     *
     * @throws ServletException
     *             the servlet exception
     */
    @Override
    public void init() throws ServletException {
        super.init();
        addMonkeys();
        RUNNER.start();
    }

    /**
     * Destroy.
     */
    @Override
    public void destroy() {
        RUNNER.stop();
        RUNNER.removeMonkey(BasicChaosMonkey.class);
        super.destroy();
    }
}
