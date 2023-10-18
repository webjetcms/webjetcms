package sk.iway.iwcm.editor.rest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.editor.service.LinkCheckService;

@Getter
@Setter
@AllArgsConstructor
public class LinkCheckErrorDto {
    private LinkCheckService.ErrorTypes errorType;
    private String link;
}
