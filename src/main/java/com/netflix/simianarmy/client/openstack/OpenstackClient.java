package com.netflix.simianarmy.client.openstack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.Validate;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.domain.Credentials;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.http.HttpRequest;
import org.jclouds.io.payloads.BaseMutableContentMetadata;
import org.jclouds.io.payloads.ByteSourcePayload;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.cinder.v1.CinderApi;
import org.jclouds.openstack.cinder.v1.features.VolumeApi;
import org.jclouds.openstack.keystone.v2_0.domain.Access;
import org.jclouds.openstack.keystone.v2_0.domain.Endpoint;
import org.jclouds.openstack.keystone.v2_0.domain.Service;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.SecurityGroup;
import org.jclouds.openstack.nova.v2_0.domain.ServerWithSecurityGroups;
import org.jclouds.openstack.nova.v2_0.domain.VolumeAttachment;
import org.jclouds.openstack.nova.v2_0.extensions.SecurityGroupApi;
import org.jclouds.openstack.nova.v2_0.extensions.VolumeAttachmentApi;
import org.jclouds.ssh.SshClient;
import org.jclouds.ssh.jsch.JschSshClient;
import org.jclouds.ssh.jsch.config.JschSshClientModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonServiceException;
import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.io.ByteSource;
import com.google.common.io.Closeables;
import com.google.common.net.HostAndPort;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.netflix.simianarmy.CloudClient;
import com.netflix.simianarmy.NotFoundException;
import com.netflix.simianarmy.client.aws.AWSClient;

/**
 * The Class OpenstackClient. Openstack client interface.
 */
public class OpenstackClient extends AWSClient implements CloudClient {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(OpenstackClient.class);
    private final OpenstackServiceConnection connection;

    private ComputeService compute = null;
    private NovaApi nova = null;

    private ComputeServiceContext context = null;
    private Access access;
    private CinderApi cinder = null;
    private HashMap<String, ArrayList<String>> endpoints;

    /**
     * Create the specific Client from the given connection information.
     *
     * @param conn
     *            connection parameters
     */
    public OpenstackClient(final OpenstackServiceConnection conn) {
        super(conn.getZone());
        connection = conn;
    }

    /**
     * Connect to the Openstack services.
     *
     * @throws AmazonServiceException
     */
    protected void connect() throws AmazonServiceException {
        try {
            final Iterable<Module> modules = ImmutableSet
                    .<Module>of(new SLF4JLoggingModule());
            final String identity = connection.getTenantName() + ":"
                    + connection.getUserName(); // tenantName:userName
            final ContextBuilder cb = ContextBuilder
                    .newBuilder(connection.getProvider())
                    .endpoint(connection.getUrl())
                    // "http://141.142.237.5:5000/v2.0/"
                    .credentials(identity, connection.getPassword())
                    .modules(modules);
            context = cb.buildView(ComputeServiceContext.class);
            compute = context.getComputeService();
            final Function<Credentials, Access> auth = context
                    .utils()
                    .injector()
                    .getInstance(
                            Key.get(new TypeLiteral<Function<Credentials, Access>>() {
                            }));
            access = auth.apply(new Credentials.Builder<Credentials>()
                    .identity(identity).credential(connection.getPassword())
                    .build());
            nova = cb.buildApi(NovaApi.class);
            cinder = ContextBuilder.newBuilder("openstack-cinder")
                    .endpoint(connection.getUrl())
                    // "http://141.142.237.5:5000/v2.0/"
                    .credentials(identity, connection.getPassword())
                    .modules(modules).buildApi(CinderApi.class);
            endpoints = new HashMap<String, ArrayList<String>>();
            for (final Service service : access) {
                // System.out.println(" Service = " + service.getName());
                endpoints.put(service.getName(), new ArrayList<String>());
                for (final Endpoint endpoint : service) {
                    endpoints.get(service.getName()).add(
                            endpoint.getPublicURL().toString());
                }
            }

        } catch (final NoSuchElementException e) {
            throw new AmazonServiceException("Cannot connect to OpenStack", e);
        }
    }

    /**
     * Disconnect from the Openstack services.
     */
    protected void disconnect() {
        try {
            Closeables.close(nova, true);
            nova = null;
        } catch (final IOException e) {
            OpenstackClient.LOGGER.error("Error disconnecting nova: "
                    + e.getMessage());
        }
        try {
            Closeables.close(cinder, true);
        } catch (final IOException e) {
            OpenstackClient.LOGGER.error("Error disconnecting cinder: "
                    + e.getMessage());
        }
    }

    /**
     * Get all API endpoints for the Openstack services.
     *
     * @return endpoints The endpoints
     */
    public HashMap<String, ArrayList<String>> getEndpoints() {
        return endpoints;
    }

    /** {@inheritDoc} */
    @Override
    public void terminateInstance(final String instanceId) {
        Validate.notEmpty(instanceId);
        connect();
        try {
            nova.getServerApiForZone(connection.getZone()).stop(instanceId);
        } catch (final UnsupportedOperationException e) {
            throw new NotFoundException(
                    "Instance " + instanceId + " not found", e);
        }
        disconnect();
    }

    /** {@inheritDoc} */
    @Override
    public void deleteAutoScalingGroup(final String asgName) {
        Validate.notEmpty(asgName);
        OpenstackClient.LOGGER
                .error("No AutoScalingGroups in OpenStack... Better wait for Heat to be released!");
    }

    /** {@inheritDoc} */
    @Override
    public void deleteLaunchConfiguration(final String launchConfigName) {
        Validate.notEmpty(launchConfigName);
        OpenstackClient.LOGGER
                .error("No AutoScalingGroups in OpenStack... Better wait for Heat to be released!");
    }

    /** {@inheritDoc} */
    @Override
    public void deleteVolume(final String volumeId) {
        Validate.notEmpty(volumeId);
        connect();
        final VolumeApi v = (VolumeApi) nova
                .getVolumeExtensionForZone(connection.getZone());
        v.delete(volumeId);
        disconnect();
    }

    /** {@inheritDoc} */
    @Override
    public void deleteSnapshot(final String snapshotId) {
        Validate.notEmpty(snapshotId);
        connect();
        cinder.getSnapshotApiForZone(connection.getZone()).delete(snapshotId);
        disconnect();
    }

    /** {@inheritDoc} */
    @Override
    public void deleteImage(final String imageId) {
        Validate.notEmpty(imageId);
        connect();
        nova.getImageApiForZone(connection.getZone()).delete(imageId);
        disconnect();
    }

    /** {@inheritDoc} */
    @Override
    public void createTagsForResources(final Map<String, String> keyValueMap,
            final String... resourceIds) {
        OpenstackClient.LOGGER.error("No tagging in OpenStack yet...");
    }

    /** {@inheritDoc} */
    @Override
    public List<String> listAttachedVolumes(final String instanceId,
            final boolean includeRoot) {
        // Returns list of volume IDs that are attached to server instanceId.
        // includeRoot doesn't do anything right now because I'm not sure how
        // Openstack handles root volumes on attached storage
        Validate.notEmpty(instanceId);
        final List<String> out = new ArrayList<String>();
        connect();
        final VolumeAttachmentApi volumeAttachmentApi = nova
                .getVolumeAttachmentExtensionForZone(connection.getZone())
                .get();

        for (final VolumeAttachment volumeAttachment : volumeAttachmentApi
                .listAttachmentsOnServer(instanceId)) {
            out.add(volumeAttachment.getVolumeId());
        }
        disconnect();
        return out;
    }

    /** {@inheritDoc} */
    @Override
    public void detachVolume(final String instanceId, final String volumeId,
            final boolean force) {
        // Detaches the volume. Openstack doesn't seem to have a force option
        // for detaching, so the force parameter will be unused.
        Validate.notEmpty(instanceId);
        Validate.notEmpty(volumeId);
        connect();
        final VolumeAttachmentApi volumeAttachmentApi = nova
                .getVolumeAttachmentExtensionForZone(connection.getZone())
                .get();
        final boolean result = volumeAttachmentApi.detachVolumeFromServer(
                volumeId, instanceId);
        if (!result) {
            OpenstackClient.LOGGER.error("Error detaching volume " + volumeId
                    + " from " + instanceId);
        }
        disconnect();
    }

    /** {@inheritDoc} */
    @Override
    public ComputeService getJcloudsComputeService() {
        return compute;
    }

    /** {@inheritDoc} */
    @Override
    public String getJcloudsId(final String instanceId) {
        Validate.notEmpty(instanceId);
        return connection.getZone() + "/" + instanceId;
    }

    /** {@inheritDoc} */
    @Override
    public String findSecurityGroup(final String instanceId,
            final String groupName) {
        Validate.notEmpty(instanceId);
        Validate.notEmpty(groupName);
        String id = null;
        connect();
        final SecurityGroupApi v = nova.getSecurityGroupExtensionForZone(
                connection.getZone()).get();
        for (final SecurityGroup group : v.list()) {
            if (group.getName() == groupName) {
                id = group.getId();
                break;
            }
        }
        disconnect();
        return id;
    }

    /** {@inheritDoc} */
    @Override
    public String createSecurityGroup(final String instanceId,
            final String groupName, final String description) {
        Validate.notEmpty(instanceId);
        Validate.notEmpty(groupName);
        Validate.notEmpty(description);
        connect();
        final SecurityGroupApi v = nova.getSecurityGroupExtensionForZone(
                connection.getZone()).get();
        OpenstackClient.LOGGER.info(String.format(
                "Creating OpenStack security group %s.", groupName));
        for (final SecurityGroup group : v.list()) {
            if (group.getName().startsWith(groupName)) {
                addSecurityGroupToInstanceByName(instanceId, groupName);
                return group.getId();
            }
        }
        final SecurityGroup result = v.createWithDescription(groupName,
                description);
        // Add security group to the instance
        addSecurityGroupToInstanceByName(instanceId, groupName);

        disconnect();
        return result.getId();
    }

    /** {@inheritDoc} */
    @Override
    public void setInstanceSecurityGroups(final String instanceId,
            final List<String> groupIds) {
        Validate.notEmpty(instanceId);
        Validate.notEmpty(groupIds);
        connect();

        // Get all security groups for instance
        final ServerWithSecurityGroups serverWithSG = nova
                .getServerWithSecurityGroupsExtensionForZone(
                        connection.getZone()).get().get(instanceId);
        // Remove all security groups from the instance
        for (final String secGroup : serverWithSG.getSecurityGroupNames()) {
            removeSecurityGroupFromInstanceByName(instanceId, secGroup);
        }
        // Add specified groups to the instance

        for (final String groupId : groupIds) {
            addSecurityGroupToInstanceById(instanceId, groupId);
        }
    }

    /**
     * Remove an instance from a security group (This assumes you have already
     * done a call to connect()).
     *
     * @param instanceId
     *            Instance identifier
     * @param groupName
     *            Group name
     */
    private void removeSecurityGroupFromInstanceByName(final String instanceId,
            final String groupName) {
        final ServerWithSecurityGroups serverWithSG = nova
                .getServerWithSecurityGroupsExtensionForZone(
                        connection.getZone()).get().get(instanceId);
        if (!serverWithSG.getSecurityGroupNames().contains(groupName)) {
            return;
        }
        modifySecurityGroupOnInstanceByName(instanceId, groupName,
                "removeSecurityGroup");
    }

    /**
     * Add an instance to a security group (This assumes you have already done a
     * call to connect()).
     *
     * @param instanceId
     *            Instance identifier
     * @param groupId
     *            Group identifier
     */
    private void addSecurityGroupToInstanceById(final String instanceId,
            final String groupId) {
        final SecurityGroupApi v = nova.getSecurityGroupExtensionForZone(
                connection.getZone()).get();
        final String groupName = v.get(groupId).getName();
        addSecurityGroupToInstanceByName(instanceId, groupName);
    }

    /**
     * Add an instance to a security group (This assumes you have already done a
     * call to connect()).
     *
     * @param instanceId
     *            Instance identifier
     * @param groupName
     *            Group name
     */
    private void addSecurityGroupToInstanceByName(final String instanceId,
            final String groupName) {
        final ServerWithSecurityGroups serverWithSG = nova
                .getServerWithSecurityGroupsExtensionForZone(
                        connection.getZone()).get().get(instanceId);
        if (serverWithSG.getSecurityGroupNames().contains(groupName)) {
            return;
        }
        modifySecurityGroupOnInstanceByName(instanceId, groupName,
                "addSecurityGroup");
    }

    /**
     * Change the security groups of an instance (This assumes you have already
     * done a call to connect()).
     *
     * @param instanceId
     *            Instance identifier
     * @param groupName
     *            Group name
     * @param operation
     *            Operation that has to be performed on the instance
     */
    private void modifySecurityGroupOnInstanceByName(final String instanceId,
            final String groupName, final String operation) {
        final String endpoint = (String) endpoints.get("nova").toArray()[0];

        final HashMultimap<String, String> headers = HashMultimap.create();
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");
        headers.put("X-Auth-Token", access.getToken().getId());
        final String requestString = "{\"" + operation + "\": {\"name\": \""
                + groupName + "\"}}";
        final ByteSource bs = ByteSource.wrap(requestString.getBytes());
        OpenstackClient.LOGGER.info("ByteSource = " + bs.toString());
        final ByteSourcePayload bsp = new ByteSourcePayload(bs);
        final BaseMutableContentMetadata meta = new BaseMutableContentMetadata();
        meta.setContentLength((long) requestString.getBytes().length);
        headers.put("Content-Length",
                String.valueOf((requestString.getBytes().length)));
        bsp.setContentMetadata(meta);
        OpenstackClient.LOGGER.info("ByteSourcePayload = " + bsp.toString());
        final HttpRequest request = HttpRequest.builder().method("POST")
                .endpoint(endpoint + "/servers/" + instanceId + "/action")
                .headers(headers).payload(bsp).build();
        context.utils().http().invoke(request);
    }

    /** {@inheritDoc} */
    @Override
    public SshClient connectSsh(final String instanceId,
            final LoginCredentials credentials) {
        connect();
        final Injector i = Guice.createInjector(new JschSshClientModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        Names.bindProperties(binder(), new Properties());
                    }
                });
        final ComputeService computeService = getJcloudsComputeService();

        final String jcloudsId = getJcloudsId(instanceId);
        NodeMetadata node = getJcloudsNode(computeService, jcloudsId);

        node = NodeMetadataBuilder.fromNodeMetadata(node)
                .credentials(credentials).build();

        final SshClient.Factory factory = i
                .getInstance(SshClient.Factory.class);
        final JschSshClient ssh = JschSshClient.class.cast(factory.create(
                HostAndPort.fromParts(
                        node.getPrivateAddresses().toArray()[0].toString(),
                        node.getLoginPort()), credentials));
        ssh.connect();
        disconnect();
        return ssh;
    }

    /**
     * Get meta-data for Node.
     *
     * @param computeService
     *            Openstack Nova ComputeService
     * @param jcloudsId
     *            Instance identifier
     * @return NodeMetaData
     */
    private NodeMetadata getJcloudsNode(final ComputeService computeService,
            final String jcloudsId) {
        // Work around a jclouds bug / documentation issue...
        // TODO: Figure out what's broken, and eliminate this function

        // This should work (?):
        // Set<NodeMetadata> nodes =
        // computeService.listNodesByIds(Collections.singletonList(jcloudsId));

        final Set<NodeMetadata> nodes = Sets.newHashSet();
        for (final ComputeMetadata n : computeService.listNodes()) {
            if (jcloudsId.equals(n.getId())) {
                nodes.add((NodeMetadata) n);
            }
        }

        if (nodes.isEmpty()) {
            OpenstackClient.LOGGER.warn("Unable to find jclouds node: {}",
                    jcloudsId);
            for (final ComputeMetadata n : computeService.listNodes()) {
                OpenstackClient.LOGGER.info("Did find node: {}", n);
            }
            throw new IllegalStateException(
                    "Unable to find node using jclouds: " + jcloudsId);
        }
        final NodeMetadata node = Iterables.getOnlyElement(nodes);
        return node;
    }

    /** {@inheritDoc} */
    @Override
    public boolean canChangeInstanceSecurityGroups(final String instanceId) {
        OpenstackClient.LOGGER
                .info("This feature requires Heat to fail. Returning true.");
        return true;
    }

    /**
     * Get NovaApi object.
     *
     * @return NovaApi
     */
    public NovaApi getNovaApi() {
        return nova;
    }

    /**
     * Get the Service Connection.
     *
     * @return OpenstackServiceConnection
     */
    public OpenstackServiceConnection getServiceConnection() {
        return connection;
    }

}

