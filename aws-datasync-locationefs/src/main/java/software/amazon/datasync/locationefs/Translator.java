package software.amazon.datasync.locationefs;

import software.amazon.awssdk.services.datasync.model.*;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class Translator {

    public static CreateLocationEfsRequest translateToCreateRequest(final ResourceModel model) {
        return CreateLocationEfsRequest.builder()
                .ec2Config(translateEC2Config(model.getEC2Config()))
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
                .maxResults(100)
                .nextToken(nextToken)
                .build();
    }

    public static DescribeLocationEfsRequest translateToReadRequest(final String locationArn) {
        return DescribeLocationEfsRequest.builder()
                .locationArn(locationArn)
                .build();
    }

    // Convert EC2Config object to Ec2Config object
    public static Ec2Config translateEC2Config(final EC2Config ec2Config) {
        return Ec2Config.builder()
                .securityGroupArns(ec2Config.getSecurityGroupArns())
                .subnetArn(ec2Config.getSubnetArn())
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
