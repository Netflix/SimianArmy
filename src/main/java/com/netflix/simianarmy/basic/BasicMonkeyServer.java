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

import java.util.List;
import java.util.LinkedList;
import java.util.Properties;
import java.io.InputStream;

import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.Monkey;
import com.netflix.simianarmy.chaos.ChaosMonkey;

@SuppressWarnings("serial")
public class BasicMonkeyServer extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicMonkeyServer.class);

    private List<Monkey> monkeys = new LinkedList<Monkey>();

    public void addMonkey(Monkey monkey) {
        monkeys.add(monkey);
    }

    @Override
    public void init() throws ServletException {
        super.init();

        String propFile = System.getProperty("simianarmy.properties", "/simianarmy.properties");
        Properties props = new Properties();
        try {
            InputStream is = BasicMonkeyServer.class.getResourceAsStream(propFile);
            try {
                props.load(is);
            } finally {
                is.close();
            }
        } catch (Exception e) {
            LOGGER.error("Unable to load properties file " + propFile
                    + " set System property \"simianarmy.properties\" to valid file");
            throw new ServletException(e.getMessage());
        }

        BasicContext ctx = new BasicContext(props);
        addMonkey(new ChaosMonkey(ctx));

        for (Monkey monkey : monkeys) {
            LOGGER.info("Starting " + monkey.type().name() + " Monkey");
            monkey.start();
        }
    }

    @Override
    public void destroy() {
        for (Monkey monkey : monkeys) {
            LOGGER.info("Stopping " + monkey.type().name() + " Monkey");
            monkey.stop();
        }
        super.destroy();
    }
}
