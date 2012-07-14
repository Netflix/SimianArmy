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
package com.netflix.simianarmy;

import java.util.List;
import java.util.ListIterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.lang.reflect.Constructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum MonkeyRunner {

    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger(MonkeyRunner.class);

    public static MonkeyRunner getInstance() {
        return INSTANCE;
    }

    public void start() {
        for (Monkey monkey : monkeys) {
            LOGGER.info("Starting " + monkey.type().name() + " Monkey");
            monkey.start();
        }
    }

    public void stop() {
        for (Monkey monkey : monkeys) {
            LOGGER.info("Stopping " + monkey.type().name() + " Monkey");
            monkey.stop();
        }
    }

    // SUPPRESS CHECKSTYLE LineLength
    private Map<Class<? extends Monkey>, Class<? extends Monkey.Context>> monkeyMap = new HashMap<Class<? extends Monkey>, Class<? extends Monkey.Context>>();
    private List<Monkey> monkeys = new LinkedList<Monkey>();

    public List<Monkey> getMonkeys() {
        return Collections.unmodifiableList(monkeys);
    }

    // simple monkey with no Context class
    public void addMonkey(Class<? extends Monkey> monkeyClass) {
        addMonkey(monkeyClass, null);
    }

    public void replaceMonkey(Class<? extends Monkey> monkeyClass) {
        replaceMonkey(monkeyClass, null);
    }

    public void addMonkey(Class<? extends Monkey> monkeyClass, Class<? extends Monkey.Context> ctxClass) {
        if (monkeyMap.containsKey(monkeyClass)) {
            throw new RuntimeException(monkeyClass.getName()
                    + " already registered, use replaceMonkey instead of addMonkey");
        }
        monkeyMap.put(monkeyClass, ctxClass);
        monkeys.add(factory(monkeyClass, ctxClass));
    }

    public void replaceMonkey(Class<? extends Monkey> monkeyClass, Class<? extends Monkey.Context> ctxClass) {
        monkeyMap.put(monkeyClass, ctxClass);
        ListIterator<Monkey> li = monkeys.listIterator();
        while (li.hasNext()) {
            Monkey monkey = li.next();
            if (monkey.getClass() == monkeyClass) {
                li.set(factory(monkeyClass, ctxClass));
                return;
            }
        }
        Monkey monkey = factory(monkeyClass, ctxClass);
        monkeys.add(monkey);
    }

    public void removeMonkey(Class<? extends Monkey> monkeyClass) {
        ListIterator<Monkey> li = monkeys.listIterator();
        while (li.hasNext()) {
            Monkey monkey = li.next();
            if (monkey.getClass() == monkeyClass) {
                monkey.stop();
                li.remove();
                break;
            }
        }
        monkeyMap.remove(monkeyClass);
    }

    public <T extends Monkey> T factory(Class<T> monkeyClass) {
        Class<? extends Monkey.Context> ctxClass = getContextClass(monkeyClass);
        if (ctxClass == null) {
            // look for derived class already in our map
            for (Map.Entry<Class<? extends Monkey>, Class<? extends Monkey.Context>> pair : monkeyMap.entrySet()) {
                if (monkeyClass.isAssignableFrom(pair.getKey())) {
                    @SuppressWarnings("unchecked")
                    T monkey = (T) factory(pair.getKey(), pair.getValue());
                    return monkey;
                }
            }
        }
        return factory(monkeyClass, ctxClass);
    }

    public <T extends Monkey> T factory(Class<T> monkeyClass, Class<? extends Monkey.Context> contextClass) {
        try {
            if (contextClass == null) {
                // assume Monkey class has has void ctor
                return monkeyClass.newInstance();
            }

            // then find corresponding ctor
            for (Constructor<?> ctor : monkeyClass.getDeclaredConstructors()) {
                Class<?>[] paramTypes = ctor.getParameterTypes();
                if (paramTypes.length != 1) {
                    continue;
                }
                if (paramTypes[0].getName().endsWith("$Context")) {
                    @SuppressWarnings("unchecked")
                    T monkey = (T) ctor.newInstance(contextClass.newInstance());
                    return monkey;
                }
            }
        } catch (Exception e) {
            LOGGER.error("monkeyFactory error, cannot make monkey from " + monkeyClass.getName() + " with "
                    + (contextClass == null ? null : contextClass.getName()), e);
        }

        return null;
    }

    public Class<? extends Monkey.Context> getContextClass(Class<? extends Monkey> monkeyClass) {
        return monkeyMap.get(monkeyClass);
    }
}
