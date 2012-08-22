package com.netflix.simianarmy;

/**
 * The Class InstanceGroupNotFoundException.
 *
 * These exceptions will be thrown when an instance group cannot be found with the
 * given name and type.
 */
public class InstanceGroupNotFoundException extends Exception {

    private static final long serialVersionUID = -5492120875166280476L;

    private final String groupType;
    private final String groupName;

    /**
     * Instantiates an InstanceGroupNotFoundException with the group type and name.
     * @param groupType the group type
     * @param groupName the gruop name
     */
    public InstanceGroupNotFoundException(String groupType, String groupName) {
        super(errorMessage(groupType, groupName));
        this.groupType = groupType;
        this.groupName = groupName;
    }

    @Override
    public String toString() {
        return errorMessage(groupType, groupName);
    }

    private static String errorMessage(String groupType, String groupName) {
        return String.format("Instance group named '%s' [type %s] cannot be found.",
                groupName, groupType);
    }
}
