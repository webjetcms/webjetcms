package sk.iway.iwcm.admin.upload;

import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.lang.Nullable;

import java.util.Arrays;
import java.util.List;

public class CustomComponentScanExclude  implements TypeFilter {

    @Override
    public boolean match(MetadataReader metadataReader,
                         @Nullable MetadataReaderFactory metadataReaderFactory) {
        ClassMetadata classMetadata = metadataReader.getClassMetadata();
        String fullyQualifiedName = classMetadata.getClassName();
        List<String> excludes = Arrays.asList(
                "sk.iway.basecms.SpringConfig",
                "sk.iway.basecms.contact.ContactRestController");
        return excludes.contains(fullyQualifiedName);
    }
}