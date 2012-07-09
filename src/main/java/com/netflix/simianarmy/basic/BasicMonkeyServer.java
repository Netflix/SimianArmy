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

import java.util.concurrent.Callable;

import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.Monkey;
import com.netflix.simianarmy.MonkeyRunner;
import com.netflix.simianarmy.chaos.ChaosMonkey;

@SuppressWarnings("serial")
public class BasicMonkeyServer extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicMonkeyServer.class);
    private static final MonkeyRunner RUNNER = MonkeyRunner.getInstance();

    static {
        RUNNER.addMonkey(ChaosMonkey.class, BasicContext.class);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        RUNNER.start();

    }

    @Override
    public void destroy() {
        RUNNER.stop();
        super.destroy();
    }

    public static class ContextListener implements ServletContextListener {
        @Override
        public void contextInitialized(ServletContextEvent sce) {
            for (Monkey monkey : RUNNER.getMonkeys()) {
                final Class<? extends Monkey> monkeyClass = monkey.getClass();
                final Class<? extends Monkey.Context> ctxClass = RUNNER.getContextClass(monkeyClass);
                Callable<Monkey> c = new Callable<Monkey>() {
                    public Monkey call() {
                        return RUNNER.factory(monkeyClass, ctxClass);
                    }
                };
                sce.getServletContext().setAttribute(monkey.type().name().toLowerCase() + "MonkeyFactory", c);
            }
        }

        @Override
        public void contextDestroyed(ServletContextEvent sce) {
            for (Monkey monkey : RUNNER.getMonkeys()) {
                sce.getServletContext().removeAttribute(monkey.type().name().toLowerCase() + "MonkeyFactory");
            }
        }
    }
}
