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

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The MonkeyRunner Singleton.
 */
public enum MonkeyRunner {

    /** The instance. */
    INSTANCE;

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MonkeyRunner.class);

    /**
     * Gets the single instance of MonkeyRunner.
     *
     * @return single instance of MonkeyRunner
     */
    public static MonkeyRunner getInstance() {
        return INSTANCE;
    }

    /**
     * Start all the monkeys registered with addMonkey or replaceMonkey.
     */
    public void start() {
        for (Monkey monkey : monkeys) {
            LOGGER.info("Starting " + monkey.type().name() + " Monkey");
            monkey.start();
        }
    }

    /**
     * Stop all of the registered monkeys.
     */
    public void stop() {
        for (Monkey monkey : monkeys) {
            LOGGER.info("Stopping " + monkey.type().name() + " Monkey");
            monkey.stop();
        }
    }

    /**
     * The monkey map. Maps the monkey class to the context class that is registered. This is so we can create new
     * monkeys in factory() that have the same context types as the registered ones.
     */
    private final Map<Class<? extends Monkey>, Class<? extends Monkey.Context>> monkeyMap =
            new HashMap<Class<? extends Monkey>, Class<? extends Monkey.Context>>();

    /** The monkeys. */
    private final List<Monkey> monkeys = new LinkedList<Monkey>();

    /**
     * Gets the registered monkeys.
     *
     * @return the monkeys
     */
    public List<Monkey> getMonkeys() {
        return Collections.unmodifiableList(monkeys);
    }

    /**
     * Adds a simple monkey void constructor.
     *
     * @param monkeyClass
     *            the monkey class
     */
    public void addMonkey(Class<? extends Monkey> monkeyClass) {
        addMonkey(monkeyClass, null);
    }

    /**
     * Replace a simple monkey that has void constructor.
     *
     * @param monkeyClass
     *            the monkey class
     */
    public void replaceMonkey(Class<? extends Monkey> monkeyClass) {
        replaceMonkey(monkeyClass, null);
    }

    /**
     * Adds the monkey.
     *
     * @param monkeyClass
     *            the monkey class
     * @param ctxClass
     *            the context class that is passed to the monkey class constructor.
     */
    public void addMonkey(Class<? extends Monkey> monkeyClass, Class<? extends Monkey.Context> ctxClass) {
        if (monkeyMap.containsKey(monkeyClass)) {
            throw new RuntimeException(monkeyClass.getName()
                    + " already registered, use replaceMonkey instead of addMonkey");
        }
        monkeyMap.put(monkeyClass, ctxClass);
        monkeys.add(factory(monkeyClass, ctxClass));
    }

    /**
     * Replace monkey. If a monkey is already registered this will replace that registered monkey.
     *
     * @param monkeyClass
     *            the monkey class
     * @param ctxClass
     *            the context class that is passed to the monkey class constructor.
     */
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

    /**
     * Removes the monkey. factory() will no longer be able to construct monkeys of the specified monkey class.
     *
     * @param monkeyClass
     *            the monkey class
     */
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

    /**
     * Monkey factory. This will generate a new monkey object of the monkeyClass type. If a monkey of monkeyClass has
     * not been registered then this will attempt to find a registered subclass and create an object of that type.
     * Example:
     *
     * <pre>
     *         {@code
     *         MonkeyRunner.getInstance().addMonkey(BasicChaosMonkey.class, BasicMonkeyContext.class);
     *         // This will actually return a BasicChaosMonkey since that is the only subclass that was registered
     *         ChaosMonkey monkey = MonkeyRunner.getInstance().factory(ChaosMonkey.class);
     *}
     * </pre>
     *
     * @param <T>
     *            the generic type, must be a subclass of Monkey
     * @param monkeyClass
     *            the monkey class
     * @return the monkey
     */
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

    /**
     * Monkey Factory. Given a monkey class and a monkey context class it will generate a new monkey. If the
     * contextClass is null it will try to generate a new monkeyClass with a void constructor;
     *
     * @param <T>
     *            the generic type, must be a subclass of Monkey
     * @param monkeyClass
     *            the monkey class
     * @param contextClass
     *            the context class
     * @return the monkey
     */
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

    /**
     * Gets the context class. You should not need this.
     *
     * @param monkeyClass
     *            the monkey class
     * @return the context class or null if a monkeyClass has not been registered
     */
    public Class<? extends Monkey.Context> getContextClass(Class<? extends Monkey> monkeyClass) {
        return monkeyMap.get(monkeyClass);
    }
}
