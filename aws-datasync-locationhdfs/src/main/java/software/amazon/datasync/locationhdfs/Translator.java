package software.amazon.datasync.locationhdfs;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.datasync.model.CreateLocationHdfsRequest;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.DescribeLocationHdfsRequest;
import software.amazon.awssdk.services.datasync.model.DeleteLocationRequest;
import software.amazon.awssdk.services.datasync.model.HdfsNameNode;
import software.amazon.awssdk.services.datasync.model.ListLocationsRequest;
import software.amazon.awssdk.services.datasync.model.QopConfiguration;
import software.amazon.awssdk.services.datasync.model.UpdateLocationHdfsRequest;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Translator {
    private static final String PRESIGNED_URL_PREFIX = "https://";

    public static CreateLocationHdfsRequest translateToCreateRequest(final ResourceModel model, Map<String, String> tags) {
        return CreateLocationHdfsRequest.builder()
                .nameNodes(translateToDataSyncNameNodes(model.getNameNodes()))
                .blockSize(model.getBlockSize() != null ? model.getBlockSize().intValue() : null)
                .replicationFactor(model.getReplicationFactor() != null ? model.getReplicationFactor().intValue() : null)
                .kmsKeyProviderUri(model.getKmsKeyProviderUri())
                .qopConfiguration(translateToDataSyncQopConfiguration(model.getQopConfiguration()))
                .authenticationType(model.getAuthenticationType())
                .simpleUser(model.getSimpleUser())
                .kerberosPrincipal(model.getKerberosPrincipal())
                .kerberosKeytab(translateToSdkBytesKeytab(model.getKerberosKeytab()))
                .kerberosKrb5Conf(translateToSdkBytesKrb5Conf(model.getKerberosKrb5Conf()))
                .tags(TagTranslator.translateMapToTagListEntries(tags))
                .agentArns(model.getAgentArns())
                .subdirectory(model.getSubdirectory())
                .build();
    }

    public static DeleteLocationRequest translateToDeleteRequest(final ResourceModel model) {
        return DeleteLocationRequest.builder()
                .locationArn(model.getLocationArn())
                .build();
    }

    public static DescribeLocationHdfsRequest translateToReadRequest(final String locationArn) {
        return DescribeLocationHdfsRequest.builder()
                .locationArn(locationArn)
                .build();
    }

    public static ListLocationsRequest translateToListRequest(final String nextToken) {
        return ListLocationsRequest.builder()
                .nextToken(nextToken)
                .build();
    }

    public static UpdateLocationHdfsRequest translateToUpdateRequest(final ResourceModel model) {
        return UpdateLocationHdfsRequest.builder()
                .locationArn(model.getLocationArn())
                .subdirectory(model.getSubdirectory())
                .nameNodes(translateToDataSyncNameNodes(model.getNameNodes()))
                .blockSize(model.getBlockSize() != null ? model.getBlockSize().intValue() : null)
                .replicationFactor(model.getReplicationFactor() != null ? model.getReplicationFactor().intValue() : null)
                .kmsKeyProviderUri(model.getKmsKeyProviderUri())
                .qopConfiguration(translateToDataSyncQopConfiguration(model.getQopConfiguration()))
                .authenticationType(model.getAuthenticationType())
                .simpleUser(model.getSimpleUser())
                .kerberosPrincipal(model.getKerberosPrincipal())
                .kerberosKeytab(translateToSdkBytesKeytab(model.getKerberosKeytab()))
                .kerberosKrb5Conf(translateToSdkBytesKrb5Conf(model.getKerberosKrb5Conf()))
                .agentArns(model.getAgentArns())
                .build();
    }

    public static List<NameNode> translateToResourceModelNameNodes(
            List<HdfsNameNode> hdfsNameNodes) {
        if (hdfsNameNodes == null) {
            return Collections.emptyList();
        }
        return hdfsNameNodes.stream()
                .map(node -> NameNode.builder()
                        .hostname(node.hostname())
                        .port(node.port())
                        .build())
                .collect(Collectors.toList());
    }

    public static software.amazon.datasync.locationhdfs.QopConfiguration translateToResourceModelQopConfiguration(QopConfiguration qopConfiguration) {
        if (qopConfiguration == null) {
            return null;
        }
        return software.amazon.datasync.locationhdfs.QopConfiguration.builder()
                .rpcProtection(qopConfiguration.rpcProtectionAsString())
                .dataTransferProtection(qopConfiguration.dataTransferProtectionAsString())
                .build();
    }

    // If the model contains a presigned URL and Kerberos Auth, but the request property is null,
    // then the model must have an invalid or expired presigned URL.
    public static boolean hasInvalidPresignedUrl(ResourceModel model, CreateLocationHdfsRequest request) {
        return modelContainsPresignedUrl(model) && model.getAuthenticationType().equals("KERBEROS")
                && (request.kerberosKrb5Conf() == null);
    }

    private static boolean modelContainsPresignedUrl(ResourceModel model) {
        return model.getKerberosKrb5Conf() != null && model.getKerberosKrb5Conf().startsWith(PRESIGNED_URL_PREFIX);
    }

    private static QopConfiguration translateToDataSyncQopConfiguration(software.amazon.datasync.locationhdfs.QopConfiguration qopConfiguration) {
        if (qopConfiguration == null) {
            return null;
        }
        return QopConfiguration.builder()
                .rpcProtection(qopConfiguration.getRpcProtection())
                .dataTransferProtection(qopConfiguration.getDataTransferProtection())
                .build();
    }

    private static List<HdfsNameNode> translateToDataSyncNameNodes(List<NameNode> nameNodes) {
        if (nameNodes == null) {
            return Collections.emptyList();
        }
        return nameNodes.stream()
                .map(node -> HdfsNameNode.builder()
                        .hostname(node.getHostname())
                        .port(node.getPort())
                        .build())
                .collect(Collectors.toList());
    }

    /*
        Since CloudFormation templates cannot contain binary text, we expect the KerberosKeytab
        property to contain a Base64 string representation of the binary keytab file.
        This will translate the Base64 representation to an SdkBytes object.
     */
    private static SdkBytes translateToSdkBytesKeytab(String modelKeytab) {
        if (modelKeytab == null) {
            return null;
        }
        byte[] decodedKeytab = Base64.getDecoder().decode(modelKeytab);
        return SdkBytes.fromByteArray(decodedKeytab);
    }

    /*
         This will translate the input for krb5.conf to an SdkBytes object. If the user inputs
         a presigned URL, it will obtain the S3 object associated with that URL and convert
         it to SdkBytes. Otherwise, this means the user input the string representation of
         the Krb5.conf file.
     */
    private static SdkBytes translateToSdkBytesKrb5Conf(String modelKrb5Conf) {
        if (modelKrb5Conf == null) {
            return null;
        } else if (modelKrb5Conf.startsWith(PRESIGNED_URL_PREFIX)) {
            return resolvePresignedUrl(modelKrb5Conf);
        } else {
            return SdkBytes.fromUtf8String(modelKrb5Conf);
        }
    }

    private static SdkBytes resolvePresignedUrl(String presignedUrl) {
        try {
            URL url = new URL(presignedUrl);
            InputStream inputStream = url.openStream();
            return SdkBytes.fromInputStream(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null; // return null on exception
    }

    public static BaseHandlerException translateDataSyncExceptionToCfnException(DataSyncException e) {
        if (e.isThrottlingException()) {
            return new CfnThrottlingException(e);
        } else {
            return new CfnGeneralServiceException(e.getMessage(), e.getCause());
        }
    }
}
