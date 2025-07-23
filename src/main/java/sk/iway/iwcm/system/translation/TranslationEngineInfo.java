package sk.iway.iwcm.system.translation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TranslationEngineInfo {
    private String engineName;
    private long remainingCharacters;
}
