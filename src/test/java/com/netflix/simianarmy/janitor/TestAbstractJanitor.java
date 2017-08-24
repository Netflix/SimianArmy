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
// CHECKSTYLE IGNORE Javadoc
// CHECKSTYLE IGNORE MagicNumberCheck

package com.netflix.simianarmy.janitor;

import com.netflix.simianarmy.*;
import com.netflix.simianarmy.Resource.CleanupState;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.janitor.rule.TestMonkeyCalendar;
import com.netflix.simianarmy.basic.BasicConfiguration;
import com.netflix.simianarmy.basic.janitor.BasicJanitorRuleEngine;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;


public class TestAbstractJanitor extends AbstractJanitor {

    private static final String TEST_REGION = "test-region";

    public TestAbstractJanitor(AbstractJanitor.Context ctx, ResourceType resourceType) {
        super(ctx, resourceType);
        this.idToResource = new HashMap<String, Resource>();
        for (Resource r : ((TestJanitorCrawler) (ctx.janitorCrawler())).getCrawledResources()) {
            this.idToResource.put(r.getId(), r);
        }
    }

    // The collection of all resources for testing.
    private final Map<String, Resource> idToResource;

    private final HashSet<String> markedResourceIds = new HashSet<String>();
    private final HashSet<String> cleanedResourceIds = new HashSet<String>();

    @Override
    protected void postMark(Resource resource) {
        markedResourceIds.add(resource.getId());
    }

    @Override
    protected void cleanup(Resource resource) {
        if (!idToResource.containsKey(resource.getId())) {
            throw new RuntimeException();
        }
        // add a special case to throw exception
        if (resource.getId().equals("11")) {
            throw new RuntimeException("Magic number of id.");
        }
        idToResource.remove(resource.getId());
    }

    @Override
    public void cleanupDryRun(Resource resource) throws DryRunnableJanitorException {
        // simulates a dryRun
        try {
            if (!idToResource.containsKey(resource.getId())) {
                throw new RuntimeException();
            }

            if (resource.getId().equals("11")) {
                throw new RuntimeException("Magic number of id.");
            }
        } catch (Exception e) {
            throw new DryRunnableJanitorException("Exception during dry run", e);
        }
    }

    @Override
    protected void postCleanup(Resource resource) {
        cleanedResourceIds.add(resource.getId());
    }

    private static List<Resource> generateTestingResources(int n) {
        List<Resource> resources = new ArrayList<Resource>(n);
        for (int i = 1; i <= n; i++) {
            resources.add(new AWSResource().withId(String.valueOf(i))
                    .withRegion(TEST_REGION)
                    .withResourceType(TestResourceType.TEST_RESOURCE_TYPE)
                    .withOptOutOfJanitor(false));
        }
        return resources;
    }

    @Test
    public static void testJanitor() {
        Collection<Resource> crawledResources = new ArrayList<Resource>();
        int n = 10;
        for (Resource r : generateTestingResources(n)) {
            crawledResources.add(r);
        }
        TestJanitorCrawler crawler = new TestJanitorCrawler(crawledResources);
        TestJanitorResourceTracker resourceTracker = new TestJanitorResourceTracker(
                new HashMap<String, Resource>());
        TestAbstractJanitor janitor = new TestAbstractJanitor(
                new TestJanitorContext(TEST_REGION,
                        new BasicJanitorRuleEngine().addRule(new IsEvenRule()),
                        crawler,
                        resourceTracker,
                        new TestMonkeyCalendar()), TestResourceType.TEST_RESOURCE_TYPE);
        janitor.setLeashed(false);
        Assert.assertEquals(
                crawler.resources(TestResourceType.TEST_RESOURCE_TYPE).size(),
                n);
        Assert.assertEquals(janitor.markedResourceIds.size(), 0);
        janitor.markResources();
        Assert.assertEquals(janitor.getMarkedResources().size(), n / 2);
        Assert.assertEquals(janitor.markedResourceIds.size(), n / 2);
        for (int i = 1; i <= n; i += 2) {
            Assert.assertTrue(janitor.markedResourceIds.contains(String.valueOf(i)));
        }

        Assert.assertEquals(janitor.cleanedResourceIds.size(), 0);
        janitor.cleanupResources();
        Assert.assertEquals(janitor.getCleanedResources().size(), n / 2);
        Assert.assertEquals(janitor.getFailedToCleanResources().size(), 0);
        Assert.assertEquals(resourceTracker.getResources(
                TestResourceType.TEST_RESOURCE_TYPE, CleanupState.JANITOR_TERMINATED, TEST_REGION).size(),
                n / 2);
        Assert.assertEquals(janitor.cleanedResourceIds.size(), n / 2);
        for (int i = 1; i <= n; i += 2) {
            Assert.assertTrue(janitor.cleanedResourceIds.contains(String.valueOf(i)));
        }
        Assert.assertEquals(janitor.getResourcesCleanedCount(), janitor.cleanedResourceIds.size());
        Assert.assertEquals(janitor.getMarkedResourcesCount(), janitor.markedResourceIds.size());
        Assert.assertEquals(janitor.getFailedToCleanResourcesCount(), 0);
    }

    @Test
    public static void testJanitorWithOptedOutResources() {
        Collection<Resource> crawledResources = new ArrayList<Resource>();
        int n = 10;
        for (Resource r : generateTestingResources(n)) {
            crawledResources.add(r);
        }
        TestJanitorCrawler crawler = new TestJanitorCrawler(crawledResources);
        // set some resources in the tracker as opted out
        Date now = new Date(DateTime.now().minusDays(1).getMillis());
        Map<String, Resource> trackedResources = new HashMap<String, Resource>();
        for (Resource r : generateTestingResources(n)) {
            int id = Integer.parseInt(r.getId());
            if (id % 4 == 1 || id % 4 == 2) {
                r.setOptOutOfJanitor(true);
                r.setState(CleanupState.MARKED);
                r.setExpectedTerminationTime(now);
                r.setMarkTime(now);
            }
            trackedResources.put(r.getId(), r);
        }
        TestJanitorResourceTracker resourceTracker = new TestJanitorResourceTracker(
                trackedResources);
        TestAbstractJanitor janitor = new TestAbstractJanitor(
                new TestJanitorContext(TEST_REGION,
                        new BasicJanitorRuleEngine().addRule(new IsEvenRule()),
                        crawler,
                        resourceTracker,
                        new TestMonkeyCalendar()), TestResourceType.TEST_RESOURCE_TYPE);
        janitor.setLeashed(false);
        Assert.assertEquals(
                crawler.resources(TestResourceType.TEST_RESOURCE_TYPE).size(),
                10);
        Assert.assertEquals(resourceTracker.getResources(
                TestResourceType.TEST_RESOURCE_TYPE, CleanupState.MARKED, TEST_REGION).size(),
                6); // 1, 2, 5, 6, 9, 10 are marked
        Assert.assertEquals(janitor.markedResourceIds.size(), 0);
        janitor.markResources();
        Assert.assertEquals(resourceTracker.getResources(
                TestResourceType.TEST_RESOURCE_TYPE, CleanupState.MARKED, TEST_REGION).size(),
                5); // 1, 3, 5, 7, 9 are marked
        Assert.assertEquals(janitor.getMarkedResources().size(), 2); // 3, 7 are newly marked.
        Assert.assertEquals(janitor.markedResourceIds.size(), 2);
        Assert.assertEquals(janitor.cleanedResourceIds.size(), 0);
        Assert.assertEquals(resourceTracker.getResources(
                TestResourceType.TEST_RESOURCE_TYPE, CleanupState.MARKED, TEST_REGION).size(),
                5); // 1, 3, 5, 7, 9 are marked
        Assert.assertEquals(janitor.getUnmarkedResources().size(), 3); // 2, 6, 10 got unmarked
        Assert.assertEquals(resourceTracker.getResources(
                TestResourceType.TEST_RESOURCE_TYPE, CleanupState.UNMARKED, TEST_REGION).size(),
                3);
        janitor.cleanupResources();
        Assert.assertEquals(janitor.getCleanedResources().size(), 2); // 3, 7 are cleaned
        Assert.assertEquals(janitor.getFailedToCleanResources().size(), 0);
        Assert.assertEquals(resourceTracker.getResources(
                TestResourceType.TEST_RESOURCE_TYPE, CleanupState.JANITOR_TERMINATED, TEST_REGION).size(),
                2);
        Assert.assertEquals(janitor.getResourcesCleanedCount(), janitor.cleanedResourceIds.size());
        Assert.assertEquals(janitor.getMarkedResourcesCount(), janitor.markedResourceIds.size());
        Assert.assertEquals(janitor.getFailedToCleanResourcesCount(), 0);
        Assert.assertEquals(janitor.getUnmarkedResourcesCount(), 3);
    }

    @Test
    public static void testJanitorWithCleanupFailure() {
        Collection<Resource> crawledResources = new ArrayList<Resource>();
        int n = 20;
        for (Resource r : generateTestingResources(n)) {
            crawledResources.add(r);
        }
        TestJanitorCrawler crawler = new TestJanitorCrawler(crawledResources);
        TestAbstractJanitor janitor = new TestAbstractJanitor(
                new TestJanitorContext(TEST_REGION,
                        new BasicJanitorRuleEngine().addRule(new IsEvenRule()),
                        crawler,
                        new TestJanitorResourceTracker(new HashMap<String, Resource>()),
                        new TestMonkeyCalendar()), TestResourceType.TEST_RESOURCE_TYPE);
        janitor.setLeashed(false);
        Assert.assertEquals(
                crawler.resources(TestResourceType.TEST_RESOURCE_TYPE).size(),
                n);
        janitor.markResources();
        Assert.assertEquals(janitor.getMarkedResources().size(), n / 2);

        janitor.cleanupResources();
        Assert.assertEquals(janitor.getCleanedResources().size(), n / 2 - 1);
        Assert.assertEquals(janitor.getFailedToCleanResources().size(), 1);
        Assert.assertEquals(janitor.getResourcesCleanedCount(), janitor.cleanedResourceIds.size());
        Assert.assertEquals(janitor.getMarkedResourcesCount(), janitor.markedResourceIds.size());
        Assert.assertEquals(janitor.getFailedToCleanResourcesCount(), 1);
    }

    private static TestAbstractJanitor getJanitor(int numberOfCrawledResources, boolean leashed) {
        TestJanitorCrawler crawler = new TestJanitorCrawler(generateTestingResources(numberOfCrawledResources));
        JanitorRuleEngine rulesEngine = new BasicJanitorRuleEngine().addRule(new IsEvenRule());
        JanitorResourceTracker resourceTracker = new TestJanitorResourceTracker(new HashMap<>());
        TestJanitorContext janitorContext = new TestJanitorContext(TEST_REGION, rulesEngine, crawler, resourceTracker, new TestMonkeyCalendar());
        TestAbstractJanitor janitor = new TestAbstractJanitor(janitorContext, TestResourceType.TEST_RESOURCE_TYPE);
        janitor.setLeashed(leashed);
        return janitor;
    }

    @Test
    public static void testCleanupDryRunOnWithJanitorOnLeashWithAFailure() {
        int n = 20;
        TestAbstractJanitor janitor = getJanitor(n, true);
        janitor.markResources();
        Assert.assertEquals(janitor.getMarkedResources().size(), n / 2);

        janitor.cleanupResources();

        Assert.assertEquals(janitor.getCleanedResources().size(), 0);
        Assert.assertEquals(janitor.getFailedToCleanResources().size(), 0);
        Assert.assertEquals(janitor.getResourcesCleanedCount(), 0);
        Assert.assertEquals(janitor.getFailedToCleanResourcesCount(), 0);
        Assert.assertEquals(janitor.getCleanupDryRunFailureCount().getValue().intValue(), 1);
    }

    @Test
    public static void testJanitorWithUnmarking() {
        Collection<Resource> crawledResources = new ArrayList<Resource>();
        Map<String, Resource> trackedResources = new HashMap<String, Resource>();
        int n = 10;
        DateTime now = DateTime.now();
        Date markTime = new Date(now.minusDays(5).getMillis());
        Date notifyTime = new Date(now.minusDays(4).getMillis());
        Date terminationTime = new Date(now.minusDays(1).getMillis());
        for (Resource r : generateTestingResources(n)) {
            if (Integer.parseInt(r.getId()) % 3 == 0) {
                trackedResources.put(r.getId(), r);
                r.setState(CleanupState.MARKED);
                r.setMarkTime(markTime);
                r.setExpectedTerminationTime(terminationTime);
                r.setNotificationTime(notifyTime);
            }
        }
        for (Resource r : generateTestingResources(n)) {
            crawledResources.add(r);
        }

        TestJanitorCrawler crawler = new TestJanitorCrawler(crawledResources);
        TestJanitorResourceTracker resourceTracker = new TestJanitorResourceTracker(trackedResources);
        TestAbstractJanitor janitor = new TestAbstractJanitor(
                new TestJanitorContext(TEST_REGION,
                        new BasicJanitorRuleEngine().addRule(new IsEvenRule()),
                        crawler,
                        resourceTracker,
                        new TestMonkeyCalendar()), TestResourceType.TEST_RESOURCE_TYPE);
        janitor.setLeashed(false);
        Assert.assertEquals(
                crawler.resources(TestResourceType.TEST_RESOURCE_TYPE).size(),
                n);
        Assert.assertEquals(resourceTracker.getResources(
                TestResourceType.TEST_RESOURCE_TYPE, CleanupState.MARKED, TEST_REGION).size(),
                n / 3);
        janitor.markResources();
        // (n/3-n/6) resources were already marked, so in the last run the marked resources
        // should be n/2 - n/3 + n/6.
        Assert.assertEquals(janitor.getMarkedResources().size(), n / 2 - n / 3 + n / 6);
        Assert.assertEquals(janitor.getUnmarkedResources().size(), n / 6);

        janitor.cleanupResources();
        Assert.assertEquals(janitor.getCleanedResources().size(), n / 2);
        Assert.assertEquals(janitor.getFailedToCleanResources().size(), 0);
        Assert.assertEquals(janitor.getResourcesCleanedCount(), janitor.cleanedResourceIds.size());
        Assert.assertEquals(janitor.getMarkedResourcesCount(), janitor.markedResourceIds.size());
        Assert.assertEquals(janitor.getFailedToCleanResourcesCount(), 0);
        Assert.assertEquals(janitor.getUnmarkedResourcesCount(), n/6);
    }


    @Test
    public static void testJanitorWithFutureTerminationTime() {
        Collection<Resource> crawledResources = new ArrayList<Resource>();
        Map<String, Resource> trackedResources = new HashMap<String, Resource>();
        int n = 10;
        DateTime now = DateTime.now();
        Date markTime = new Date(now.minusDays(5).getMillis());
        Date notifyTime = new Date(now.minusDays(4).getMillis());
        Date terminationTime = new Date(now.plusDays(10).getMillis());
        for (Resource r : generateTestingResources(n)) {
            trackedResources.put(r.getId(), r);
            r.setState(CleanupState.MARKED);
            r.setNotificationTime(notifyTime);
            r.setMarkTime(markTime);
            r.setExpectedTerminationTime(terminationTime);
        }
        for (Resource r : generateTestingResources(n)) {
            crawledResources.add(r);
        }

        TestJanitorCrawler crawler = new TestJanitorCrawler(crawledResources);
        TestJanitorResourceTracker resourceTracker = new TestJanitorResourceTracker(trackedResources);

        TestAbstractJanitor janitor = new TestAbstractJanitor(
                new TestJanitorContext(TEST_REGION,
                        new BasicJanitorRuleEngine().addRule(new IsEvenRule()),
                        crawler,
                        resourceTracker,
                        new TestMonkeyCalendar()), TestResourceType.TEST_RESOURCE_TYPE);
        janitor.setLeashed(false);
        Assert.assertEquals(resourceTracker.getResources(
                TestResourceType.TEST_RESOURCE_TYPE, CleanupState.MARKED, TEST_REGION).size(),
                n);
        janitor.cleanupResources();
        Assert.assertEquals(janitor.getCleanedResources().size(), 0);
        Assert.assertEquals(janitor.getFailedToCleanResources().size(), 0);
        Assert.assertEquals(janitor.getResourcesCleanedCount(), janitor.cleanedResourceIds.size());
        Assert.assertEquals(janitor.getMarkedResourcesCount(), janitor.markedResourceIds.size());
        Assert.assertEquals(janitor.getFailedToCleanResourcesCount(), 0);
    }


    @Test
    public static void testJanitorWithoutNotification() {
        Collection<Resource> crawledResources = new ArrayList<Resource>();
        Map<String, Resource> trackedResources = new HashMap<String, Resource>();
        int n = 10;
        for (Resource r : generateTestingResources(n)) {
            trackedResources.put(r.getId(), r);
            r.setState(CleanupState.MARKED);
            // The marking/cleanup is not notified so we the Janitor won't clean it up.
            // r.setNotificationTime(new Date());
            r.setMarkTime(new Date());
            r.setExpectedTerminationTime(new Date(DateTime.now().plusDays(10).getMillis()));
        }
        for (Resource r : generateTestingResources(n)) {
            crawledResources.add(r);
        }
        TestJanitorCrawler crawler = new TestJanitorCrawler(crawledResources);
        TestJanitorResourceTracker resourceTracker = new TestJanitorResourceTracker(trackedResources);

        TestAbstractJanitor janitor = new TestAbstractJanitor(
                new TestJanitorContext(TEST_REGION,
                        new BasicJanitorRuleEngine().addRule(new IsEvenRule()),
                        crawler,
                        resourceTracker,
                        new TestMonkeyCalendar()), TestResourceType.TEST_RESOURCE_TYPE);
        janitor.setLeashed(false);
        Assert.assertEquals(resourceTracker.getResources(
                TestResourceType.TEST_RESOURCE_TYPE, CleanupState.MARKED, TEST_REGION).size(),
                n);

        janitor.cleanupResources();
        Assert.assertEquals(janitor.getCleanedResources().size(), 0);
        Assert.assertEquals(janitor.getFailedToCleanResources().size(), 0);
        Assert.assertEquals(janitor.getResourcesCleanedCount(), janitor.cleanedResourceIds.size());
        Assert.assertEquals(janitor.getMarkedResourcesCount(), janitor.markedResourceIds.size());
        Assert.assertEquals(janitor.getFailedToCleanResourcesCount(), 0);
    }

    @Test
    public static void testLeashedJanitorForMarking() {
        Collection<Resource> crawledResources = new ArrayList<Resource>();
        int n = 10;
        for (Resource r : generateTestingResources(n)) {
            crawledResources.add(r);
        }
        TestJanitorCrawler crawler = new TestJanitorCrawler(crawledResources);
        TestJanitorResourceTracker resourceTracker = new TestJanitorResourceTracker(
                new HashMap<String, Resource>());
        TestAbstractJanitor janitor = new TestAbstractJanitor(
                new TestJanitorContext(TEST_REGION,
                        new BasicJanitorRuleEngine().addRule(new IsEvenRule()),
                        crawler,
                        resourceTracker,
                        new TestMonkeyCalendar()), TestResourceType.TEST_RESOURCE_TYPE);
        janitor.setLeashed(true);
        Assert.assertEquals(
                crawler.resources(TestResourceType.TEST_RESOURCE_TYPE).size(),
                n);
        janitor.markResources();
        Assert.assertEquals(janitor.getMarkedResources().size(), n / 2);
        Assert.assertEquals(janitor.getResourcesCleanedCount(), janitor.cleanedResourceIds.size());
        Assert.assertEquals(janitor.getMarkedResourcesCount(), n / 2);
    }


    @Test
    public static void testJanitorWithoutHoldingOffCleanup() {
        Collection<Resource> crawledResources = new ArrayList<Resource>();
        int n = 10;
        for (Resource r : generateTestingResources(n)) {
            crawledResources.add(r);
        }
        TestJanitorCrawler crawler = new TestJanitorCrawler(crawledResources);
        TestJanitorResourceTracker resourceTracker = new TestJanitorResourceTracker(new HashMap<String, Resource>());
        DateTime now = DateTime.now();
        TestAbstractJanitor janitor = new TestAbstractJanitor(
                new TestJanitorContext(TEST_REGION,
                        new BasicJanitorRuleEngine().addRule(new ImmediateCleanupRule(now)),
                        crawler,
                        resourceTracker,
                        new TestMonkeyCalendar()), TestResourceType.TEST_RESOURCE_TYPE);
        janitor.setLeashed(false);
        Assert.assertEquals(
                crawler.resources(TestResourceType.TEST_RESOURCE_TYPE).size(),
                n);
        Assert.assertEquals(janitor.markedResourceIds.size(), 0);
        janitor.markResources();
        Assert.assertEquals(janitor.getMarkedResources().size(), n);
        Assert.assertEquals(janitor.markedResourceIds.size(), n);
        for (int i = 1; i <= n; i++) {
            Assert.assertTrue(janitor.markedResourceIds.contains(String.valueOf(i)));
        }

        Assert.assertEquals(janitor.cleanedResourceIds.size(), 0);
        janitor.cleanupResources();
        // No resource is cleaned since the notification is later than expected termination time.
        Assert.assertEquals(janitor.getCleanedResources().size(), n);
        Assert.assertEquals(janitor.getFailedToCleanResources().size(), 0);
        Assert.assertEquals(resourceTracker.getResources(
                TestResourceType.TEST_RESOURCE_TYPE, CleanupState.JANITOR_TERMINATED, TEST_REGION).size(),
                n);
        Assert.assertEquals(janitor.cleanedResourceIds.size(), n);
        Assert.assertEquals(janitor.getResourcesCleanedCount(), janitor.cleanedResourceIds.size());
        Assert.assertEquals(janitor.getMarkedResourcesCount(), janitor.markedResourceIds.size());
        Assert.assertEquals(janitor.getFailedToCleanResourcesCount(), 0);
    }

    @Test
    public static void testJanitorWithUnmarkingUserTerminated() {
        Collection<Resource> crawledResources = new ArrayList<Resource>();
        Map<String, Resource> trackedResources = new HashMap<String, Resource>();
        int n = 10;
        DateTime now = DateTime.now();
        Date markTime = new Date(now.minusDays(5).getMillis());
        Date notifyTime = new Date(now.minusDays(4).getMillis());
        Date terminationTime = new Date(now.minusDays(1).getMillis());
        for (Resource r : generateTestingResources(n)) {
            if (Integer.parseInt(r.getId()) % 3 != 0) {
                crawledResources.add(r);
            } else {
                trackedResources.put(r.getId(), r);
                r.setState(CleanupState.MARKED);
                r.setMarkTime(markTime);
                r.setNotificationTime(notifyTime);
                r.setExpectedTerminationTime(terminationTime);
            }
        }

        TestJanitorCrawler crawler = new TestJanitorCrawler(crawledResources);
        TestJanitorResourceTracker resourceTracker = new TestJanitorResourceTracker(trackedResources);
        TestAbstractJanitor janitor = new TestAbstractJanitor(
                new TestJanitorContext(TEST_REGION,
                        new BasicJanitorRuleEngine().addRule(new IsEvenRule()),
                        crawler,
                        resourceTracker,
                        new TestMonkeyCalendar()), TestResourceType.TEST_RESOURCE_TYPE);
        janitor.setLeashed(false);
        Assert.assertEquals(
                crawler.resources(TestResourceType.TEST_RESOURCE_TYPE).size(),
                n - n / 3);
        Assert.assertEquals(resourceTracker.getResources(
                TestResourceType.TEST_RESOURCE_TYPE, CleanupState.MARKED, TEST_REGION).size(),
                n / 3);
        janitor.markResources();
        // n/3 resources should be considered user terminated
        Assert.assertEquals(janitor.getMarkedResources().size(), n / 2 - n / 3 + n / 6);
        Assert.assertEquals(janitor.getUnmarkedResources().size(), n / 3);

        janitor.cleanupResources();
        Assert.assertEquals(janitor.getCleanedResources().size(), n / 2 - n / 3 + n / 6);
        Assert.assertEquals(janitor.getFailedToCleanResources().size(), 0);
        Assert.assertEquals(janitor.getResourcesCleanedCount(), janitor.cleanedResourceIds.size());
        Assert.assertEquals(janitor.getMarkedResourcesCount(), janitor.markedResourceIds.size());
        Assert.assertEquals(janitor.getFailedToCleanResourcesCount(), 0);
        Assert.assertEquals(janitor.getUnmarkedResourcesCount(), n / 3);
    }
}

class TestJanitorCrawler implements JanitorCrawler {
    private final Collection<Resource> crawledResources;
    public Collection<Resource> getCrawledResources() {
        return crawledResources;
    }

    public TestJanitorCrawler(Collection<Resource> crawledResources) {
        this.crawledResources = crawledResources;
    }

    @Override
    public EnumSet<? extends ResourceType> resourceTypes() {
        return EnumSet.of(TestResourceType.TEST_RESOURCE_TYPE);
    }

    @Override
    public List<Resource> resources(ResourceType resourceType) {
        return new ArrayList<Resource>(crawledResources);
    }

    @Override
    public List<Resource> resources(String... resourceIds) {
        List<Resource> result = new ArrayList<Resource>(resourceIds.length);
        Set<String> idSet = new HashSet<String>(Arrays.asList(resourceIds));
        for (Resource r : crawledResources) {
            if (idSet.contains(r.getId())) {
                result.add(r);
            }
        }
        return result;
    }

    @Override
    public String getOwnerEmailForResource(Resource resource) {
        return null;
    }
}

enum TestResourceType implements ResourceType {
    TEST_RESOURCE_TYPE
}

class TestJanitorResourceTracker implements JanitorResourceTracker {
    private final Map<String, Resource> resources;
    public TestJanitorResourceTracker(Map<String, Resource> trackedResources) {
        this.resources = trackedResources;
    }

    @Override
    public void addOrUpdate(Resource resource) {
        resources.put(resource.getId(), resource);
    }

    @Override
    public List<Resource> getResources(ResourceType resourceType, CleanupState state, String region) {
        List<Resource> result = new ArrayList<Resource>();
        for (Resource r : resources.values()) {
            if (r.getResourceType().equals(resourceType)
                    && (r.getState() != null && r.getState().equals(state))
                    && r.getRegion().equals(region)) {
                result.add(r.cloneResource());
            }
        }
        return result;
    }

    @Override
    public Resource getResource(String resourceId) {
        return resources.get(resourceId);
    }

    @Override
    public Resource getResource(String resourceId, String region) {
        return resources.get(resourceId);
    }

}

/**
 * The rule considers all resources with an odd number as the id as cleanup candidate.
 */
class IsEvenRule implements Rule {
    @Override
    public boolean isValid(Resource resource) {
        // returns true if the resource's id is an even integer
        int id;
        try {
            id = Integer.parseInt(resource.getId());
        } catch (Exception e) {
            return true;
        }
        DateTime now = DateTime.now();
        resource.setExpectedTerminationTime(new Date(now.minusDays(1).getMillis()));
        // Set the resource as notified so it can be cleaned
        // set the notification time at more than 1 day before the termination time
        resource.setNotificationTime(new Date(now.minusDays(4).getMillis()));
        return id % 2 == 0;
    }
}

/**
 * The rule considers all resources as cleanup candidate and sets notification time
 * after the termination time.
 */
class ImmediateCleanupRule implements Rule {
    private final DateTime now;
    public ImmediateCleanupRule(DateTime now) {
        this.now = now;
    }
    @Override
    public boolean isValid(Resource resource) {
        resource.setExpectedTerminationTime(new Date(now.minusMinutes(10).getMillis()));
        resource.setNotificationTime(new Date(now.getMillis()-5000));
        return false;
    }
}

class TestJanitorContext implements AbstractJanitor.Context {
    private final String region;
    private final JanitorRuleEngine ruleEngine;
    private final JanitorCrawler crawler;
    private final JanitorResourceTracker resourceTracker;
    private final MonkeyCalendar calendar;

    public TestJanitorContext(String region, JanitorRuleEngine ruleEngine, JanitorCrawler crawler,
            JanitorResourceTracker resourceTracker, MonkeyCalendar calendar) {
        this.region = region;
        this.resourceTracker = resourceTracker;
        this.ruleEngine = ruleEngine;
        this.crawler = crawler;
        this.calendar = calendar;
    }

    @Override
    public String region() {
        return region;
    }

    @Override
    public MonkeyCalendar calendar() {
        return calendar;
    }

    @Override
    public JanitorRuleEngine janitorRuleEngine() {
        return ruleEngine;
    }

    @Override
    public JanitorCrawler janitorCrawler() {
        return crawler;
    }

    @Override
    public JanitorResourceTracker janitorResourceTracker() {
        return resourceTracker;
    }

    @Override
    public MonkeyConfiguration configuration() {
        return new BasicConfiguration(new Properties());
    }

    @Override
    public MonkeyRecorder recorder() {
        // No events to be recorded
        return null;
    }
}
