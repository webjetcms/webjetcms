package sk.iway.iwcm.form;
import java.io.InputStream;

import sk.iway.iwcm.IwcmRequest;

public interface FillRequestInterceptor
{
    public boolean intercept(InputStream isFile, IwcmRequest wrapped);
}
