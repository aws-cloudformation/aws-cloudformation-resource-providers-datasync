package software.amazon.datasync.locationfsxlustre;

import software.amazon.awssdk.services.datasync.model.*;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;

import java.util.Map;

public class Translator {

    Translator() {}

    public static CreateLocationFsxLustreRequest translateToCreateRequest(final ResourceModel model, Map<String, String> tags) {
        return CreateLocationFsxLustreRequest.builder()
                .fsxFilesystemArn(model.getFsxFilesystemArn())
                .securityGroupArns(model.getSecurityGroupArns())
                .subdirectory(model.getSubdirectory())
                .tags(TagTranslator.translateMapToTagListEntries(tags))
                .build();
    }

    public static DescribeLocationFsxLustreRequest translateToReadRequest(final String locationArn) {
        return DescribeLocationFsxLustreRequest.builder()
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

    public static BaseHandlerException translateDataSyncExceptionToCfnException(DataSyncException e) {
        if (e.isThrottlingException()) {
            return new CfnThrottlingException(e);
        } else {
            return new CfnGeneralServiceException(e.getMessage(), e.getCause());
        }
    }
}
