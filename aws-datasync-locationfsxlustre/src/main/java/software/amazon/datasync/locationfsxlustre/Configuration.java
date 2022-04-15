package software.amazon.datasync.locationfsxlustre;

import java.util.Map;

class Configuration extends BaseConfiguration {

    public Configuration() {
        super("aws-datasync-locationfsxlustre.json");
    }

    @Override
    public Map<String, String> resourceDefinedTags(final ResourceModel resourceModel) {
        return TagTranslator.translateTagsToMap(resourceModel.getTags());
    }
}
