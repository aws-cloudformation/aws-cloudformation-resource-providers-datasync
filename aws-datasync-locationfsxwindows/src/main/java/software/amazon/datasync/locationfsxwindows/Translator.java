package software.amazon.datasync.locationfsxwindows;

import software.amazon.awssdk.services.datasync.model.CreateLocationFsxWindowsRequest;
import software.amazon.awssdk.services.datasync.model.DeleteLocationRequest;
import software.amazon.awssdk.services.datasync.model.DescribeLocationFsxWindowsRequest;
import software.amazon.awssdk.services.datasync.model.ListLocationsRequest;
import software.amazon.awssdk.services.datasync.model.TagListEntry;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Translator {

    Translator() {}

    public static CreateLocationFsxWindowsRequest translateToCreateRequest(final ResourceModel model, Map<String, String> tags) {
        return CreateLocationFsxWindowsRequest.builder()
                .domain(model.getDomain())
                .fsxFilesystemArn(model.getFsxFilesystemArn())
                .password(model.getPassword())
                .securityGroupArns(model.getSecurityGroupArns())
                .subdirectory(model.getSubdirectory())
                .tags(TagTranslator.translateMapToTagListEntries(tags))
                .user(model.getUser())
                .build();
    }

    public static DescribeLocationFsxWindowsRequest translateToReadRequest(final String locationArn) {
        return DescribeLocationFsxWindowsRequest.builder()
                .locationArn(locationArn)
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

}
