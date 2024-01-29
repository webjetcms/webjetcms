package sk.iway.iwcm.update;

import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
public class UpdateForm {
    @NotNull(message = "form.document.not_null")
    private MultipartFile document;
}
