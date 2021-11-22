package software.amazon.datasync.locationefs;

import software.amazon.awssdk.services.datasync.model.CreateLocationEfsRequest;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.DeleteLocationRequest;
import software.amazon.awssdk.services.datasync.model.DescribeLocationEfsRequest;
import software.amazon.awssdk.services.datasync.model.ListLocationsRequest;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;

import java.util.Map;

public class Translator {

    public static CreateLocationEfsRequest translateToCreateRequest(final ResourceModel model, Map<String, String> tags) {
        return CreateLocationEfsRequest.builder()
                .ec2Config(translateToDataSyncEc2Config(model.getEc2Config()))
                .efsFilesystemArn(model.getEfsFilesystemArn())
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

    public static DescribeLocationEfsRequest translateToReadRequest(final String locationArn) {
        return DescribeLocationEfsRequest.builder()
                .locationArn(locationArn)
                .build();
    }

    // Convert Resource Model defined EC2 Config to a DataSync EC2 Config
    private static software.amazon.awssdk.services.datasync.model.Ec2Config translateToDataSyncEc2Config(final Ec2Config ec2Config) {
        if (ec2Config == null)
            return software.amazon.awssdk.services.datasync.model.Ec2Config.builder().build();
        return software.amazon.awssdk.services.datasync.model.Ec2Config.builder()
                .securityGroupArns(ec2Config.getSecurityGroupArns())
                .subnetArn(ec2Config.getSubnetArn())
                .build();
    }

    // Convert DataSync EC2 Config to a Resource Model defined EC2 Config
    public static Ec2Config translateToResourceModelEc2Config(final software.amazon.awssdk.services.datasync.model.Ec2Config ec2Config) {
        if (ec2Config == null)
            return Ec2Config.builder().build();
        return Ec2Config.builder()
                .securityGroupArns(ec2Config.securityGroupArns())
                .subnetArn(ec2Config.subnetArn())
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
