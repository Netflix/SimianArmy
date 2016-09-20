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

package com.netflix.simianarmy.aws;

import com.netflix.simianarmy.ResourceType;

/**
 * The enum of resource types of AWS.
 */
public enum AWSResourceType implements ResourceType {
    /** AWS instance. */
    INSTANCE,
    /** AWS EBS volume. */
    EBS_VOLUME,
    /** AWS EBS snapshot. */
    EBS_SNAPSHOT,
    /** AWS auto scaling group. */
    ASG,
    /** AWS launch configuration. */
    LAUNCH_CONFIG,
    /** AWS S3 bucket. */
    S3_BUCKET,
    /** AWS security group. */
    SECURITY_GROUP,
    /** AWS Amazon Machine Image. **/
    IMAGE,
    /** AWS Elastic Load Balancer. **/
    ELB
}
