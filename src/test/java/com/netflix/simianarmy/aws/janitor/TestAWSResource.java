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
package com.netflix.simianarmy.aws.janitor;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.AWSResourceType;

public class TestAWSResource {
    /** Make sure the getFieldToValue returns the right field and values.
     * @throws Exception **/
    @Test
    public void testFieldToValueMapWithoutNullForInstance() throws Exception {
        Date now = new Date();
        Resource resource = getTestingResource(now);

        Map<String, String> resourceFieldValueMap = resource.getFieldToValueMap();

        verifyMapsAreEqual(resourceFieldValueMap,
                getTestingFieldValueMap(now, getTestingFields()));
    }

    /**
     * When all fields are null, the map returned is empty.
     */
    @Test
    public void testFieldToValueMapWithNull() {
        Resource resource = new AWSResource();
        Map<String, String> resourceFieldValueMap = resource.getFieldToValueMap();
        // The only value in the map is the boolean of opt out
        Assert.assertEquals(resourceFieldValueMap.size(), 1);
    }

    @Test
    public void testParseFieldToValueMap() throws Exception {
        Date now = new Date();
        Map<String, String> map = getTestingFieldValueMap(now, getTestingFields());
        AWSResource resource = AWSResource.parseFieldtoValueMap(map);

        Map<String, String> resourceFieldValueMap = resource.getFieldToValueMap();

        verifyMapsAreEqual(resourceFieldValueMap, map);
    }

    @Test
    public void testClone() {
        Date now = new Date();
        Resource resource = getTestingResource(now);
        Resource clone = resource.cloneResource();
        verifyMapsAreEqual(clone.getFieldToValueMap(), resource.getFieldToValueMap());
        verifyTagsAreEqual(clone, resource);
    }

    private void verifyMapsAreEqual(Map<String, String> map1, Map<String, String> map2) {
        Assert.assertFalse(map1 == null ^ map2 == null);
        Assert.assertEquals(map1.size(), map2.size());
        for (Map.Entry<String, String> entry : map1.entrySet()) {
            Assert.assertEquals(entry.getValue(), map2.get(entry.getKey()));
        }
    }

    private void verifyTagsAreEqual(Resource r1, Resource r2) {
        Collection<String> keys1 = r1.getAllTagKeys();
        Collection<String> keys2 = r2.getAllTagKeys();
        Assert.assertEquals(keys1.size(), keys2.size());

        for (String key : keys1) {
            Assert.assertEquals(r1.getTag(key), r2.getTag(key));
        }
    }

    private Map<String, String> getTestingFieldValueMap(Date defaultDate, Map<String, String> additionalFields)
            throws Exception {
        Field[] fields = AWSResource.class.getFields();
        Map<String, String> fieldToValue = new HashMap<String, String>();

        String dateString = AWSResource.DATE_FORMATTER.print(defaultDate.getTime());
        for (Field field : fields) {
            if (field.getName().startsWith("FIELD_")) {
                String value;
                String key = (String) (field.get(null));
                if (field.getName().endsWith("TIME")) {
                    value = dateString;
                } else if (field.getName().equals("FIELD_STATE")) {
                    value = "MARKED";
                } else if (field.getName().equals("FIELD_RESOURCE_TYPE")) {
                    value = "INSTANCE";
                } else if (field.getName().equals("FIELD_OPT_OUT_OF_JANITOR")) {
                    value = "false";
                } else {
                    value = (String) (field.get(null));
                }
                fieldToValue.put(key, value);
            }
        }
        if (additionalFields != null) {
            fieldToValue.putAll(additionalFields);
        }
        return fieldToValue;
    }

    private Resource getTestingResource(Date now) {
        String id = "resourceId";
        Resource resource = new AWSResource().withId(id).withRegion("region").withResourceType(AWSResourceType.INSTANCE)
                .withState(Resource.CleanupState.MARKED).withDescription("description")
                .withExpectedTerminationTime(now).withActualTerminationTime(now)
                .withLaunchTime(now).withMarkTime(now).withNnotificationTime(now).withOwnerEmail("ownerEmail")
                .withTerminationReason("terminationReason").withOptOutOfJanitor(false);
        ((AWSResource) resource).setAWSResourceState("awsResourceState");

        for (Map.Entry<String, String> field : getTestingFields().entrySet()) {
            resource.setAdditionalField(field.getKey(), field.getValue());
        }

        for (int i = 1; i < 10; i++) {
            resource.setTag("tagKey_" + i, "tagValue_" + i);
        }

        return resource;
    }

    private Map<String, String> getTestingFields() {
        Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i < 10; i++) {
            map.put("name" + i, "value" + i);
        }
        return map;
    }
}
