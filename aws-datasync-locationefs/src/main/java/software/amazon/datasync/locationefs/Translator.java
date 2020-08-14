package software.amazon.datasync.locationefs;

import software.amazon.awssdk.services.datasync.model.CreateLocationEfsRequest;
import software.amazon.awssdk.services.datasync.model.DeleteLocationRequest;
import software.amazon.awssdk.services.datasync.model.DescribeLocationEfsRequest;
import software.amazon.awssdk.services.datasync.model.Ec2Config;
import software.amazon.awssdk.services.datasync.model.ListLocationsRequest;
import software.amazon.awssdk.services.datasync.model.TagListEntry;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class Translator {

    public static CreateLocationEfsRequest translateToCreateRequest(final ResourceModel model) {
        return CreateLocationEfsRequest.builder()
                .ec2Config(translateToDataSyncEC2Config(model.getEC2Config()))
                .efsFilesystemArn(model.getEfsFilesystemArn())
                .subdirectory(model.getSubdirectory())
                .tags(translateTags(model.getTags()))
                .build();
    }


    public static DeleteLocationRequest translateToDeleteRequest(final ResourceModel model) {
        return DeleteLocationRequest.builder()
                .locationArn(model.getLocationArn())
                .build();
    }

    public static ListLocationsRequest translateToListRequest(final String nextToken) {
        return ListLocationsRequest.builder()
                .nextToken(nextToken)
                .build();
    }

    public static DescribeLocationEfsRequest translateToReadRequest(final String locationArn) {
        return DescribeLocationEfsRequest.builder()
                .locationArn(locationArn)
                .build();
    }

    // Convert Resource Model defined EC2 Config to a DataSync EC2 Config
    private static Ec2Config translateToDataSyncEC2Config(final EC2Config ec2Config) {
        if (ec2Config == null)
            return Ec2Config.builder().build();
        return Ec2Config.builder()
                .securityGroupArns(ec2Config.getSecurityGroupArns())
                .subnetArn(ec2Config.getSubnetArn())
                .build();
    }

    // Convert DataSync EC2 Config to a Resource Model defined EC2 Config
    public static EC2Config translateToResourceModelEc2Config(final Ec2Config ec2Config) {
        if (ec2Config == null)
            return EC2Config.builder().build();
        return EC2Config.builder()
                .securityGroupArns(ec2Config.securityGroupArns())
                .subnetArn(ec2Config.subnetArn())
                .build();
    }

    // Convert Tag object to TagListEntry object
    public static Set<TagListEntry> translateTags(final Set<Tag> tags) {
        if (tags == null)
            return Collections.emptySet();
        return tags.stream()
                .map(tag -> TagListEntry.builder().key(tag.getKey()).value(tag.getValue()).build())
                .collect(Collectors.toSet());
    }
}
