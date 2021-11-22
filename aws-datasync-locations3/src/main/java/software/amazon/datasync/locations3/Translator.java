package software.amazon.datasync.locations3;

import software.amazon.awssdk.services.datasync.model.CreateLocationS3Request;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.DeleteLocationRequest;
import software.amazon.awssdk.services.datasync.model.DescribeLocationS3Request;
import software.amazon.awssdk.services.datasync.model.ListLocationsRequest;
import software.amazon.awssdk.services.datasync.model.S3Config;
import software.amazon.awssdk.services.datasync.model.TagListEntry;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Translator {

    Translator() {}

    public static CreateLocationS3Request translateToCreateRequest(final ResourceModel model, Map<String, String> tags) {
        return CreateLocationS3Request.builder()
                .s3BucketArn(model.getS3BucketArn())
                .s3Config(translateToDataSyncS3Config(model.getS3Config()))
                .s3StorageClass(model.getS3StorageClass())
                .subdirectory(model.getSubdirectory())
                .tags(TagTranslator.translateMapToTagListEntries(tags))
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

    public static DescribeLocationS3Request translateToReadRequest(final String locationArn) {
        return DescribeLocationS3Request.builder()
                .locationArn(locationArn)
                .build();
    }

    public static software.amazon.datasync.locations3.S3Config translateToModelS3Config(S3Config s3Config) {
        return software.amazon.datasync.locations3.S3Config.builder()
                .bucketAccessRoleArn(s3Config.bucketAccessRoleArn())
                .build();
    }

    // Convert S3Config object to the correct type for DataSync API
    private static S3Config translateToDataSyncS3Config(software.amazon.datasync.locations3.S3Config s3Config) {
        return S3Config.builder()
                .bucketAccessRoleArn(s3Config.getBucketAccessRoleArn())
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
