package software.amazon.datasync.agent;

import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.cloudformation.LambdaWrapper;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClientBuilder {

    /*
        • LambdaWrapper.HTTP_CLIENT builds an ApacheHttpClient
        • This gets inputted to build an instance of DataSyncClient
     */
    public static DataSyncClient getClient() {
        return DataSyncClient.builder()
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .build();
    }
}
