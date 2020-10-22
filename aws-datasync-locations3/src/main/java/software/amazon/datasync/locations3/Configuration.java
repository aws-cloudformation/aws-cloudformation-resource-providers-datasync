package software.amazon.datasync.locations3;

import java.util.Map;

class Configuration extends BaseConfiguration {

    public Configuration() {
        super("aws-datasync-locations3.json");
    }

    @Override
    public Map<String, String> resourceDefinedTags(final ResourceModel resourceModel) {
        return TagTranslator.translateTagsToMap(resourceModel.getTags());
    }

}
