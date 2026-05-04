package sk.iway.iwcm.system.ntlm;

import java.security.Principal;

public class NtlmPrincipal implements Principal {

    String name;
    String domain;

    public NtlmPrincipal(String domain, String name) {
        this.domain = domain;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getDomain() {
        return domain;
    }

}
