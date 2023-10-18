package sk.iway.iwcm.setup;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LicenseFormBean { 
    String username;
    String password;
    String license;

    //Param just to inform BE what language is used in FE
	private String pageLngIndicator = "sk";
}